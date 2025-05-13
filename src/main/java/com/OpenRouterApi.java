package com;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class OpenRouterApi {

    private static final String API_KEY = "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";
    private static final String OPEN_ROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    /**
     * Gera uma análise contextualizada com base no termo de pesquisa e nas citações.
     *
     * @param termo    Termo de pesquisa.
     * @param citacoes Lista de citações curtas.
     * @return A análise contextualizada gerada pela API.
     */
    public String gerarAnaliseContextualizada(String termo) {
        // Formatar o prompt para a API
        // public String gerarAnaliseContextualizada(String termo, List<String> citacoes) {
        // String prompt = "Baseado no termo de pesquisa '" + termo + "' e nas seguintes citações:\n" +String.join("\n", citacoes) +"\nGere uma análise contextualizada.";
        String prompt = "Baseado no termo de pesquisa '" + termo + "' e nas seguintes citações:\n" + "\nGere uma análise contextualizada com no máximo 300 caracteres, mas sem mostrar quantos caracteres foram utilizados.";

        // Criar o payload da requisição
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "qwen/qwen3-32b:free");
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        payload.add("messages", new Gson().toJsonTree(new JsonObject[]{message}));

        try {
            // Fazer a requisição POST para a API
            HttpResponse<String> response = Unirest.post(OPEN_ROUTER_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .body(payload.toString())
                    .asString();

            // Verificar se a resposta foi bem-sucedida
            if (response.getStatus() == 200) {
                // Parsear a resposta usando Gson
                JsonObject jsonResponse = new Gson().fromJson(response.getBody(), JsonObject.class);
                return jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else {
                System.err.println("Erro na API OpenRouter: " + response.getStatus() + " - " + response.getBody());
                return "Erro ao gerar análise contextualizada: " + response.getBody();
            }
        } catch (UnirestException e) {
            System.err.println("Erro ao se comunicar com a API OpenRouter: " + e.getMessage());
            return "Erro ao se comunicar com a API OpenRouter.";
        }
    }
}