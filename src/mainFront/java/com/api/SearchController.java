package mainFront.java.com.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.ArrayList;
import java.util.List;

@Controller
public class SearchController {

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                        @RequestParam(required = false) boolean exact,
                        Model model) {
        
        // Se não houver termo de busca, apenas mostra a página
        if (q == null || q.trim().isEmpty()) {
            return "search";
        }

        // Simulação de resultados de busca
        List<SearchResult> results = new ArrayList<>();
        
        // Adiciona alguns resultados de exemplo
        results.add(new SearchResult(
            "Exemplo de Resultado 1",
            "Este é um exemplo de descrição para o primeiro resultado da busca.",
            "https://exemplo.com/resultado1",
            "2024-03-20"
        ));
        
        results.add(new SearchResult(
            "Exemplo de Resultado 2",
            "Aqui temos outro exemplo de descrição para demonstrar os resultados da busca.",
            "https://exemplo.com/resultado2",
            "2024-03-19"
        ));

        model.addAttribute("query", q);
        model.addAttribute("exact", exact);
        model.addAttribute("results", results);
        
        return "search";
    }

    @GetMapping("/statistics")
    public String statistics() {
        return "statistics";
    }
}

class SearchResult {
    private String title;
    private String description;
    private String url;
    private String date;

    public SearchResult(String title, String description, String url, String date) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.date = date;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public String getDate() { return date; }
} 