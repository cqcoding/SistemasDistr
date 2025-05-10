package com.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.Naming;

import com.InterfaceBarrel;
import com.InterfaceGatewayServer;


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

        List<SearchResult> results = new ArrayList<>();
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

        
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Erro ao realizar a pesquisa: " + e.getMessage());
        }

        model.addAttribute("query", q);
        model.addAttribute("exact", exact);
        model.addAttribute("results", results);
        
        return "search";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        try {
            // Conectar ao servidor RMI
            String serverIp = "localhost"; // Substitua pelo IP do servidor, se necessário
            String server = "rmi://" + serverIp + "/server";
            InterfaceGatewayServer gateway = (InterfaceGatewayServer) Naming.lookup(server);
    
        
            List<String> pesquisasFrequentes = gateway.obterPesquisasMaisFrequentes();
            Map<String, Integer> barrelsAtivos = gateway.obterBarrelsAtivos();
            Map<String, Integer> tamanhoIndices = new HashMap<>();
            Map<String, Integer> temposResposta = new HashMap<>();

            // Contar o número total de URLs indexadas no arquivo
            int totalUrlsIndexadas = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader("urlsIndexados.txt"))) {
                while (reader.readLine() != null) {
                    totalUrlsIndexadas++;
                }
            } catch (IOException e) {
                System.err.println("Erro ao ler o arquivo urlsIndexados.txt: " + e.getMessage());
            }
                
            
            for (String barrel : barrelsAtivos.keySet()) {
                try {
                    InterfaceBarrel barrelProxy = (InterfaceBarrel) Naming.lookup("rmi://" + serverIp + "/" + barrel);
                    tamanhoIndices.put(barrel, barrelProxy.getTamanhoIndice());
                    temposResposta.put(barrel, barrelProxy.tamanhoFilaURLs()); 
                } catch (Exception e) {
                    System.err.println("Erro ao acessar o Barrel: " + barrel + " - " + e.getMessage());
                    tamanhoIndices.put(barrel, 0);
                    temposResposta.put(barrel, 0);
                }
            }
            
            // Adicionar dados ao modelo
            model.addAttribute("pesquisasFrequentes", pesquisasFrequentes);
            model.addAttribute("barrelsAtivos", barrelsAtivos);
            model.addAttribute("tamanhoIndices", tamanhoIndices);
            model.addAttribute("temposResposta", temposResposta);
            model.addAttribute("totalUrlsIndexadas", totalUrlsIndexadas);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Erro ao obter as estatísticas: " + e.getMessage());
            
            // Inicializar variáveis vazias em caso de erro
            model.addAttribute("pesquisasFrequentes", new ArrayList<>());
            model.addAttribute("barrelsAtivos", new HashMap<>());
            model.addAttribute("tamanhoIndices", new HashMap<>());
            model.addAttribute("temposResposta", new HashMap<>());
            model.addAttribute("totalUrlsIndexadas", 0);
            model.addAttribute("error", "Erro ao obter as estatísticas: " + e.getMessage());
        }
   
    
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
        this.citation= citation;
    }

    // Getters
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getCitation() { return citation; }
}