package com.api;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.InterfaceBarrel;
import com.InterfaceGatewayServer;
import com.api.services.RealTimeUpdateService;

import jakarta.annotation.PostConstruct;


@Controller
public class SearchController {
    // Campos para o cliente RMI e o serviço WebSocket
    private InterfaceGatewayServer gateway;
    private final RealTimeUpdateService realTimeUpdateService;

     // Injetar URLs RMI e IP do servidor Gateway a partir de application.properties
    @Value("${rmi.gateway.server.ip:localhost}") 
    private String gatewayServerIp;

    @Value("${rmi.barrel.urls:}") 
    private String[] barrelRmiUrls;


    // 1. INJEÇÃO DE DEPENDÊNCIA VIA CONSTRUTOR
    @Autowired
    public SearchController(RealTimeUpdateService realTimeUpdateService) {
        this.realTimeUpdateService = realTimeUpdateService;
        // A inicialização do RMI será feita no método initRmi com @PostConstruct
    }

    // 2. INICIALIZAÇÃO DO CLIENTE RMI
    // Este método será chamado pelo Spring após a construção do bean e injeção de dependências.
    @PostConstruct
    private void initRmiClient() {
        try {
            String serverUrl = "rmi://" + gatewayServerIp + "/server"; // "/server" é o nome com o qual o GatewayServer foi registado
            this.gateway = (InterfaceGatewayServer) Naming.lookup(serverUrl);
            System.out.println("SearchController: Conectado ao Gateway RMI em " + serverUrl);
        } catch (Exception e) {
            System.err.println("SearchController: Erro Crítico - Falha ao conectar ao Gateway RMI: " + e.getMessage());
            // Em caso de falha, o gateway permanecerá null
            this.gateway = null;
        }
    }

        @GetMapping("/search/next")
    public String nextPage(Model model) {
        try {
            if (this.gateway != null) {
                String resultados = this.gateway.next_page();
                model.addAttribute("results", parseResultados(resultados));
            }
        } catch (IOException e) {
            model.addAttribute("error", "Erro ao carregar a próxima página: " + e.getMessage());
        }
        return "search";
    }

        @GetMapping("/search/previous")
    public String previousPage(Model model) {
        try {
            if (this.gateway != null) {
                String resultados = this.gateway.previous_page();
                model.addAttribute("results", parseResultados(resultados));
            }
        } catch (IOException e) {
            model.addAttribute("error", "Erro ao carregar a página anterior: " + e.getMessage());
        }
        return "search";
    }

    // Método auxiliar para converter os resultados em objetos SearchResult
    private List<SearchResult> parseResultados(String resultados) {
        List<SearchResult> results = new ArrayList<>();
        String[] blocos = resultados.split("\n\n");
        for (String bloco : blocos) {
            String[] linhas = bloco.split("\n");
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
            results.add(new SearchResult(titulo, url, citacao));
        }
        return results;
    }


    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(required = false) boolean exact,
                         Model model) {
        // Se não houver termo de busca, apenas mostra a página
        if (q == null || q.trim().isEmpty()) {
            return "search";
        }

        // Verifica se o gateway RMI (this.gateway) está disponível
        if (this.gateway == null) {
            model.addAttribute("error", "Serviço de pesquisa indisponível no momento. Tente mais tarde.");
            model.addAttribute("query", q);
            return "search";
        }

        List<SearchResult> results = new ArrayList<>();
        try {
            // Realizar a pesquisa
            List<String> resultadosBrutos = this.gateway.pesquisar(q);

            if (resultadosBrutos != null && !resultadosBrutos.isEmpty()) {
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
                    results.add(new SearchResult(titulo, url, citacao));
                }
            }

            // Gerar análise contextualizada com base somente no termo
            String analise = this.gateway.gerarAnaliseContextualizada(q);
            // Adicionar a análise ao modelo para exibição no frontend
            model.addAttribute("analysis", analise);

        } catch (IOException e) {
            model.addAttribute("error", "Erro ao realizar a pesquisa: " + e.getMessage());
        }

        model.addAttribute("query", q);
        model.addAttribute("exact", exact);
        model.addAttribute("results", results);

        return "search";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {

        // Verificar se o gateway RMI (this.gateway) está disponível
        if (this.gateway == null) {
            model.addAttribute("error", "Serviço de estatísticas indisponível no momento. Tente mais tarde.");
            // Fornecer valores padrão para o template não quebrar
            model.addAttribute("pesquisasFrequentes", new ArrayList<>());
            model.addAttribute("barrelsAtivos", new HashMap<>());
            model.addAttribute("temposResposta", new HashMap<>());
            model.addAttribute("totalUrlsIndexadas", 0);
            model.addAttribute("totalPesquisasGlobal", 0); 
            model.addAttribute("tamanhoIndices", new HashMap<>()); 
            return "statistics";
        }

        try {
            // USA A INSTÂNCIA 'this.gateway' JÁ INICIALIZADA
            List<String> pesquisasFrequentes = this.gateway.obterPesquisasMaisFrequentes();
            Map<String, Integer> barrelsAtivos = this.gateway.obterBarrelsAtivos();

            Map<String, Long> temposRespostaMs = new HashMap<>(); // O JavaScript espera Long (ms)

            try {
                Map<String, Double> rawTemposResposta = this.gateway.obterTemposResposta(); 
                if (rawTemposResposta != null) {
                    rawTemposResposta.forEach((barrelName, tempoDouble) -> {
                        if (tempoDouble != null) {
                            // o tempoDouble está em nanosegundos, aqui converte para milissegundos
                            temposRespostaMs.put(barrelName, (long) (tempoDouble / 10.0)); 
                        }
                    });
                }
            } 
            catch (RemoteException re) {
                System.err.println("Erro ao chamar this.gateway.obterTemposResposta(): " + re.getMessage());
            } 
            catch (Exception e) { 
                 System.err.println("Erro inesperado ao processar tempos de resposta do gateway: " + e.getMessage());
            }


            Map<String, Integer> tamanhoIndicesMap = new HashMap<>(); 
            int totalUrlsIndexadasCalculado = 0;

            // obter tamanhoIndicesMap e totalUrlsIndexadasCalculado
            // Se o GatewayServer já fornecer estas informações de forma agregada, use-as.
            // Caso contrário, iterar sobre os barrels é uma opção:
            if (this.barrelRmiUrls != null && barrelsAtivos != null && !barrelsAtivos.isEmpty()) {
                for (String barrelUrl : this.barrelRmiUrls) {
                    try {
                        InterfaceBarrel barrelClient = (InterfaceBarrel) Naming.lookup(barrelUrl);
                        String nomeBarrel = barrelClient.getNomeBarrel(); 

                        if (barrelsAtivos.containsKey(nomeBarrel)) { 
                            int tamanhoIndice = barrelClient.getTamanhoIndice(); 
                            tamanhoIndicesMap.put(nomeBarrel, tamanhoIndice);
                            totalUrlsIndexadasCalculado += tamanhoIndice;

                            // Se obterTemposResposta do gateway não funcionar ou não incluir todos os barrels ativos,
                            // você pode ter um fallback aqui para calcular a latência, como antes:
                            if (!temposRespostaMs.containsKey(nomeBarrel)) {
                                long startTime = System.currentTimeMillis();
                                barrelClient.estaAtivo(); 
                                long endTime = System.currentTimeMillis();
                                temposRespostaMs.put(nomeBarrel, endTime - startTime);
                            }
                        }
                    } 
                    catch (Exception e) {
                        System.err.println("Erro ao obter dados do barrel " + barrelUrl + " para tamanho/índice: " + e.getMessage());
                    }
                }
            }
            
            // Fallback para totalUrlsIndexadasCalculado se não obtido dos barrels
            if (totalUrlsIndexadasCalculado == 0 && (tamanhoIndicesMap.isEmpty())) { 
                 try (BufferedReader reader = new BufferedReader(new FileReader("urlsIndexados.txt"))) {
                    System.out.println("Recorrendo à leitura de urlsIndexados.txt para totalUrlsIndexadasCalculado.");
                    while (reader.readLine() != null) {
                        totalUrlsIndexadasCalculado++;
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao ler o arquivo urlsIndexados.txt: " + e.getMessage());
                }
            }


            // Calcular o total de pesquisas realizadas 
            int totalPesquisas = 0;
            if (pesquisasFrequentes != null) {
                totalPesquisas = pesquisasFrequentes.stream()
                .mapToInt(p -> {
                    try {
                        return Integer.parseInt(p.split(":")[1].trim());
                        } catch (Exception e) {
                        return 0; }
                        
                    })
                    .sum();
            }
            
            // Adicionar dados ao modelo para renderização inicial
            model.addAttribute("pesquisasFrequentes", pesquisasFrequentes);
            model.addAttribute("barrelsAtivos", barrelsAtivos);
            model.addAttribute("temposResposta", temposRespostaMs); 
            model.addAttribute("totalUrlsIndexadas", totalUrlsIndexadasCalculado);
            model.addAttribute("totalPesquisasGlobal", totalPesquisas); 
            model.addAttribute("tamanhoIndices", tamanhoIndicesMap); 

            Map<String, Object> statisticsDataForWebSocket = new HashMap<>();
            statisticsDataForWebSocket.put("pesquisasFrequentes", pesquisasFrequentes);
            statisticsDataForWebSocket.put("barrelsAtivos", barrelsAtivos); 
            statisticsDataForWebSocket.put("temposResposta", temposRespostaMs); 
            statisticsDataForWebSocket.put("totalUrlsIndexadas", totalUrlsIndexadasCalculado);
            statisticsDataForWebSocket.put("totalPesquisasGlobal", totalPesquisas); 
            statisticsDataForWebSocket.put("tamanhoIndices", tamanhoIndicesMap); 
            
            this.realTimeUpdateService.sendStatisticsUpdate(statisticsDataForWebSocket);
            System.out.println("SearchController: Dados de estatísticas enviados via WebSocket.");
            // --- FIM Do WEBSOCKET ---

        } 
        catch (IOException e) {
            model.addAttribute("error", "Erro ao obter as estatísticas: " + e.getMessage());
            
            // Inicializar variáveis vazias em caso de erro
            model.addAttribute("pesquisasFrequentes", new ArrayList<>());
            model.addAttribute("barrelsAtivos", new HashMap<>());
            model.addAttribute("temposResposta", new HashMap<>());
            model.addAttribute("totalUrlsIndexadas", 0);
            model.addAttribute("totalPesquisasGlobal", 0);
            model.addAttribute("tamanhoIndices", new HashMap<>());
        }
        return "statistics";
    }

    @RequestMapping(value = "/relacoes", method = {RequestMethod.GET, RequestMethod.POST})
    public String consultarRelacoes(@RequestParam(required = false) String url, Model model) {
        if (url == null || url.isEmpty()) {
            //model.addAttribute("error", "O parâmetro 'url' é obrigatório.");
            return "relacoes";
        }

        if (this.gateway == null) {
            model.addAttribute("error", "Serviço indisponível no momento. Tente novamente mais tarde.");
            return "relacoes";
        }

        try {
            // Obter os backlinks (links que apontam para a URL fornecida) a partir do GatewayServer
            List<String> relacoes = this.gateway.consultarRelacoes(url);

            if (relacoes == null || relacoes.isEmpty()) {
                model.addAttribute("message", "Nenhuma relação encontrada para a URL fornecida.");
            } else {
                model.addAttribute("relacoes", relacoes);
            }

            model.addAttribute("url", url);
        } catch (RemoteException e) {
            model.addAttribute("error", "Erro ao consultar relações: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Erro inesperado: " + e.getMessage());
        }
        return "relacoes";
    }

    @RequestMapping(value = "/index-url", method = RequestMethod.POST)
    public String indexUrl(@RequestParam("url") String url, Model model) {
        try {
            if (this.gateway != null) {
                this.gateway.indexar_URL(url);
                model.addAttribute("success", "URL indexada com sucesso: " + url);
            } else {
                model.addAttribute("error", "Serviço indisponível. Não foi possível indexar a URL.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao indexar a URL: " + e.getMessage());
        }
        return "search"; // Retorna para a página de busca
    }

    @GetMapping("/")
    public String redirectToSearch() {
        return "redirect:/search";
    }
}