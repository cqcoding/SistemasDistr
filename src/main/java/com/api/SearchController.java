package com.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Controller
public class SearchController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${backend.gateway.url}") // URL do teu GatewayServer configurada no application.properties
    private String gatewayUrl;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(required = false) boolean exact,
                         Model model) {

        // Se não houver termo de busca, apenas mostra a página
        if (q == null || q.trim().isEmpty()) {
            return "search";
        }

        String searchResult = restTemplate.getForObject(gatewayUrl + "/api/search?query=" + q, String.class);
        model.addAttribute("query", q);
        model.addAttribute("exact", exact);
        model.addAttribute("results", searchResult); // Adapta conforme a estrutura da resposta do backend

        return "search";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        String statistics = restTemplate.getForObject(gatewayUrl + "/api/statistics", String.class);
        model.addAttribute("statistics", statistics); // Adapta conforme a estrutura da resposta do backend
        return "statistics";
    }

    @GetMapping("/search/next")
    public String nextPage(Model model) {
        String nextResults = restTemplate.getForObject(gatewayUrl + "/api/search/next", String.class);
        model.addAttribute("results", nextResults); // Mantém o nome "results" para consistência na view
        return "search"; // Ou redireciona para outra página de resultados se preferir
    }

    @GetMapping("/search/previous")
    public String previousPage(Model model) {
        String previousResults = restTemplate.getForObject(gatewayUrl + "/api/search/previous", String.class);
        model.addAttribute("results", previousResults); // Mantém o nome "results" para consistência na view
        return "search"; // Ou redireciona para outra página de resultados se preferir
    }

    @GetMapping("/search/links")
    public String linksToPage(Model model) {
        List<String> links = restTemplate.getForObject(gatewayUrl + "/api/search/links", List.class);
        model.addAttribute("links", links); // Adapta o nome conforme precisar exibir os links
        return "search"; // Ou outra página para exibir os links
    }
}