package com.api;

import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import com.InterfaceGatewayServer;

@Controller
public class SearchController {

    private final WebClient webClient;

    public SearchController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1").build();
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(required = false) boolean exact,
                         Model model) {

        // Se não houver termo de busca, apenas mostra a página
        if (q == null || q.trim().isEmpty()) {
            return "search";
        }

        List<SearchResult> results = new ArrayList<>();
        List<String> aiResults = new ArrayList<>();
        try {
            // Conectar ao servidor RMI
            String serverIp = "localhost"; // Substitua pelo IP do servidor, se necessário
            String server = "rmi://" + serverIp + "/server";
            InterfaceGatewayServer gateway = (InterfaceGatewayServer) Naming.lookup(server);

            // Realizar a pesquisa
            List<String> resultadosBrutos = gateway.pesquisar(q);

            // Converter os resultados para objetos SearchResult
            for (String resultadoBruto : resultadosBrutos) {
                String[] linhas = resultadoBruto.split("\n");
                String titulo = "Título não disponível";
                String url = "URL não disponível";
                String citacao = "Citação não disponível";

                for (String linha : linhas) {
                    if (linha.startsWith("Título: ")) {
                        titulo = linha.substring(8).trim();
                    } else if (linha.startsWith("URL: ")) {
                        url = linha.substring(5).trim();
                    } else if (linha.startsWith("Citação: ")) {
                        citacao = linha.substring(11).trim();
                    }
                }

                // Adiciona o resultado, mesmo que algum campo esteja vazio
                results.add(new SearchResult(titulo, url, citacao));
            }

            // Consultar a API da OpenAI
            aiResults = getChatCompletion(q);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Erro ao realizar a pesquisa: " + e.getMessage());
        }

        model.addAttribute("query", q);
        model.addAttribute("exact", exact);
        model.addAttribute("results", results);
        model.addAttribute("aiResults", aiResults);

        return "search";
    }

    private List<String> getChatCompletion(String prompt) {
    try {
        Map<String, Object> requestBody = Map.of(
            "model", "gpt-3.5-turbo",  // use gpt-4 apenas se tiver acesso liberado
            "messages", List.of(
                Map.of("role", "system", "content", "Você é um assistente que analisa termos de busca."),
                Map.of("role", "user", "content", prompt)
            )
        );

        Map response = this.webClient.post()
            .uri("/chat/completions")
            .header("Authorization", "Bearer " + System.getenv("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")) // recomendado usar variável de ambiente
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (response == null || !response.containsKey("choices")) {
            return List.of("A resposta da OpenAI está vazia ou malformada.");
        }

        return ((List<Map<String, Object>>) response.get("choices"))
            .stream()
            .map(choice -> (String) ((Map<String, Object>) choice.get("message")).get("content"))
            .toList();

    } catch (Exception e) {
        e.printStackTrace();
        return List.of("Erro ao consultar a OpenAI: " + e.getMessage());
    }
}

    @GetMapping("/statistics")
    public String statistics() {
        return "statistics";
    }
}

class SearchResult {
    private String title;
    private String url;
    private String citation;

    public SearchResult(String title, String url, String citation) {
        this.title = title;
        this.url = url;
        this.citation = citation;
    }

    // Getters
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getCitation() { return citation; }
}