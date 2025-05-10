package com;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class OpenAIService {

    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    public OpenAIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1").build();
    }

    public List<String> getChatCompletion(String prompt) {
        int maxRetries = 3;
        long waitTimeMs = 1000; // 1 segundo de espera inicial

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> response = this.webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                        "model", "gpt-3.5-turbo",
                        "messages", List.of(
                            Map.of("role", "system", "content", "Você é um assistente que analisa resultados de busca."),
                            Map.of("role", "user", "content", prompt)
                        )
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

                if (response == null || !response.containsKey("choices")) {
                    return List.of("Resposta vazia ou inesperada da OpenAI.");
                }

                return ((List<Map<String, Object>>) response.get("choices"))
                        .stream()
                        .map(choice -> {
                            Map<String, Object> message = (Map<String, Object>) choice.get("message");
                            return message != null ? (String) message.get("content") : "Resposta sem conteúdo.";
                        })
                        .toList();

            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    System.out.println("Tentativa " + attempt + " de " + maxRetries + " falhou com 429 Too Many Requests. Retentando em " + waitTimeMs + " ms...");
                    try {
                        Thread.sleep(waitTimeMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return List.of("Operação interrompida durante o retry.");
                    }
                    waitTimeMs *= 2; // backoff exponencial
                } else {
                    e.printStackTrace();
                    return List.of("Erro ao consultar a OpenAI: " + e.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                return List.of("Erro ao consultar a OpenAI: " + e.getMessage());
            }
        }
        return List.of("Erro: limite de requisições da OpenAI atingido após várias tentativas.");
    }
}