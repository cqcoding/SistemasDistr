package com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.api.HackerNewsItemRecord;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.web.client.RestTemplate;

/**
 * GatewayServer é responsável por gerenciar a comunicação entre os clientes e os servidores Barrel.
 * Distribui URLs para processamento, realiza buscas distribuídas, monitora a atividade dos Barrels e mantém estatísticas de uso.
 * Essa classe implementa a interface InterfaceGatewayServer e estende UnicastRemoteObject, permitindo chamadas remotas via remota.
 */

public class GatewayServer extends UnicastRemoteObject implements InterfaceGatewayServer {
// NOTE: é necessário o -extends UnicastRemoteObject- pois ele faz automaticamente a exportação dos objetos remotos para que os clientes consigam chamá-lo remotamente.
    
    /** Lista de Barrels conectados ao Gateway. */
    private List<InterfaceBarrel> barrels;

    /** Lista de URLs que já foram indexadas. */
    private static final String ArquivoURLS = "urlsIndexados.txt";
    private List<String> urlsIndexados;

    /** Estruturas necessárias para armazenar estatísticas.
     * Contagem das pesquisas mais comuns.
     */
    private final Map<String, Integer> pesquisasFrequentes;  
    
    /** Mapeia os Barrels ativos e o tamanho do índice de cada um. */
    private final Map<String, Integer> barrelsAtivos;        
    
    /** Mapeia cada Barrel ao tempo médio de resposta. */
    private final Map<String, List<Long>> temposResposta;    

    /** Estruturas necessárias para nextpage, previous e link.
     * Lista contendo os resultados da última pesquisa. 
    */
    private List<ResultadoPesquisa> resultadosPesquisa;           // usa list, pois não precisa estar associada a uma chave, são só URLs.
    
    /** Índice da página atual de resultados. */
    private int paginaAtual;
    
    /** Número de resultados exibidos por página. */                                
    private static final int TAMANHO_PAGINA = 10; 

    /** guardar as 10 pesqusias mais frequentes pra não perder quando fechar o cliente */
    private static final String arquivoPesquisas = "pesquisasFrequentes.txt";

    private Map<String, List<String>> relacoesDeUrls;
    /**
     * Construtor da classe GatewayServer.
     * Inicializa as estruturas de dados e estabelece a conexão com os Barrels disponíveis.
     * Protegido para garantir que só classes filhas ou dentro do mesmo pacote possam instanciar o objeto diretamente.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    protected GatewayServer(String serverIp, List<String> barrelUrls) throws RemoteException {    
        super();              // NOTE: exporta o objeto remoto automaticamente, sem isso, o objeto não ficaria disponível para chamadas remotas.
        this.barrels = new ArrayList<>();
        this.urlsIndexados = new ArrayList<>();    
        this.pesquisasFrequentes = new HashMap<>();     
        this.barrelsAtivos = new HashMap<>();
        this.temposResposta = new HashMap<>();
        this.resultadosPesquisa = new ArrayList<>();
        this.paginaAtual = 0;
                   
        conectarBarrels(barrelUrls);
        carregarURLs();
        // Carregar relações entre URLs usando a nova classe
        RelacoesUrlsLoader loader = new RelacoesUrlsLoader();
        this.relacoesDeUrls = loader.carregarRelacoes("urlsIndexados.txt"); 
        carregarPesquisasFrequentes(); 
        iniciarMonitoramento();

    }
        public static void main(String[] args) {
            GatewayServer server = null; 
            try {
                /** Carrega propriedades do arquivo. */
                Properties properties = new Properties();
               try (InputStream input = GatewayServer.class.getResourceAsStream("/config.properties")) {
                    if (input == null) {
                    throw new FileNotFoundException("Arquivo config.properties não encontrado no classpath.");
                    }
                properties.load(input);
            }
    
                /** Obtém o IP do servidor a partir das propriedades. */
                String serverIp = properties.getProperty("server.ip", "localhost");
                
                
                /** Obtém as URLs dos barrels a partir das propriedades. */
                String barrelUrlsString = properties.getProperty("barrel.urls");
                List<String> barrelUrls = new ArrayList<>();

                if (barrelUrlsString != null && !barrelUrlsString.trim().isEmpty()) {
                    String[] urlsArray = barrelUrlsString.split(",");
                    for (String url : urlsArray) {
                        barrelUrls.add(url.trim()); // Adiciona a URL após remover espaços em branco
                    }
                } 
                else {
                    System.err.println("Propriedade 'barrel.urls' não encontrada ou vazia em config.properties.");
                }
                
                String objName = "rmi://" + serverIp + "/server";
                
                // Passa a lista de URLs lidas do arquivo para o construtor
                server = new GatewayServer(serverIp, barrelUrls);
    
                System.out.println("Registrando objeto no RMIRegistry...");
    
                try {
                    LocateRegistry.createRegistry(1099);
                    System.out.println("Registry RMI criado na porta 1099.");
                } catch (Exception e) {
                    System.out.println("Registry RMI já existente.");
                }
    
                /** Registra o objeto remoto no RMI Registry. */
                Naming.rebind(objName, server);  
    
                System.out.println("Servidor RMI pronto...");
            } 
            catch (Exception e) {
                e.printStackTrace(); 
            } 
        }

    /**
     * Estabelece a conexão com os servidores Barrel.
     */
    private void conectarBarrels(List<String> barrelUrls) {

        barrels.clear();    

        for(String barrelUrl: barrelUrls){
            try { 
                    InterfaceBarrel barrel = (InterfaceBarrel) Naming.lookup(barrelUrl); // conecta os Barrels usando a URL fornecida.
                    barrels.add(barrel);       // adiciona o Barrel conectado à lista de Barrels.
                    System.out.println("Conectado ao barrel: " + barrelUrl);
            } 
            catch (Exception e) {
                System.err.println("Erro ao conectar aos barrels " + barrelUrl);
                e.printStackTrace();
            }
        }
    }

    /**
     * Envia uma URL para um Barrel processá-la.
     *
     * @param url URL a ser enviada para processamento.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    @Override
    public void enviarURLParaProcessamento(String url) throws RemoteException {
        if (barrels.isEmpty()) {
            System.out.println("Nenhum Barrel disponível para processar a URL.");
            return;
        }

        /** Escolher Barrel com menos URLs na fila. */
        InterfaceBarrel melhorBarrel = null;
        int menorFila = Integer.MAX_VALUE;

        for (InterfaceBarrel barrel : barrels) {
            try {
                int tamanhoFila = barrel.tamanhoFilaURLs(); // obtém o tamanho da fila de cada Barrel.
                if (tamanhoFila < menorFila) {
                    menorFila = tamanhoFila;
                    melhorBarrel = barrel;
                }
            } 
            catch (RemoteException e) {
                System.out.println("Erro ao verificar fila do Barrel: " + e.getMessage());
            }
        }

        if (melhorBarrel != null) {
            //melhorBarrel.adicionarURLNaFila(url);
            //System.out.println("URL enviada para processamento no Barrel.");

            System.out.println("Gateway: Enviando URL '" + url + "' para " + melhorBarrel.getNomeBarrel()); 
            melhorBarrel.adicionarURLNaFila(url);
            System.out.println("Gateway: URL enviada para processamento no Barrel. Novo tamanho da fila: " + melhorBarrel.tamanhoFilaURLs()); 
        } 
        else {
            System.out.println("Nenhum Barrel disponível para receber a URL.");
        }
    }

    /**
     * Inicia um monitoramento contínuo dos Barrels para verificar sua atividade.
     */
    private void iniciarMonitoramento() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // verifica a cada 5 segundos.

                    /** Limpa apenas os Barrels inativos do mapa. */
                    barrelsAtivos.entrySet().removeIf(entry -> !barrelAindaAtivo(entry.getKey()));
                    for (InterfaceBarrel barrel : barrels) {
                        try {
                            if (barrel.estaAtivo()) {               // verifica se cada Barrel está ativo.
                                String nomeBarrel = barrel.getNomeBarrel();
                                int tamanhoIndice = barrel.getTamanhoIndice();        // obtém o total indexado.
                                atualizarTamanhoBarrel(nomeBarrel, tamanhoIndice);
                            }
                        } catch (RemoteException e) {
                            System.err.println("Barrel inativo detectado!");
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();                // isso é o que realmente faz a thread começar a compilar.
    }

    /**
     * Verifica se um determinado Barrel ainda está ativo e o obtém.
     *
     * @param nome 
     * @return true se o Barrel estiver ativo, false caso contrário.
     */
    private boolean barrelAindaAtivo(String nome) {
        for (InterfaceBarrel barrel : barrels) {
            if (barrel.toString().equals(nome)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executa um Reliable Multicast para garantir que os dados sejam replicados entre os Barrels.
     * O método envia a URL a ser indexada para todos os Barrels disponíveis e aguarda confirmação de recebimento.
     * Se qualquer um dos Barrels não confirmar o recebimento, a operação é considerada falha.
     * @param palavra palavra-chave associada à URL que será indexada.
     * @param url URL a ser indexada nos Barrels.
     * @return true se todos os Barrels confirmarem a indexação com sucesso, false caso contrário.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    public boolean reliableMulticast(String palavra, String url) {
        List<InterfaceBarrel> confirmados = new ArrayList<>();

        try {
            // faz o envio da URL para os Barrels disponíveis.
            for (InterfaceBarrel barrel : barrels) {
                barrel.indexar_URL(palavra, url);
            }

            // confirma o recebimento ou a operação falha caso qualquer Barrel não confirmar.
            for (InterfaceBarrel barrel : barrels) {
                if (barrel.confirmarRecebimento(palavra, url)) {
                    confirmados.add(barrel);
                } else {
                    System.err.println("Erro: Barrel não confirmou recebimento."); 
                    return false;
                }
            }

            System.out.println("Indexação concluída com sucesso!");
            return true;
        } catch (RemoteException e) {
            System.err.println("Falha no Reliable Multicast: " + e.getMessage());
            return false;
        }
    }

    /**
     * Indexa uma URL em todos os Barrels disponíveis - indexar = salvar.
     *
     * @param url URL a ser indexada.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    @Override
    public void indexar_URL(String url) throws RemoteException {
        System.out.println("Recebendo solicitação para indexar URL: " + url);

        // Verificar se a URL já foi processada
        if (urlsIndexados.contains(url)) {
            System.out.println("URL já indexada: " + url);
            return;
        }
    
        // Adicionar a URL ao arquivo e processá-la
        try {
            Downloader downloader = new Downloader(10); // Número de threads
            downloader.comecaProcessarURL(url); // Processar a URL diretamente
            System.out.println("URL enviada para processamento: " + url);
        } catch (Exception e) {
            System.err.println("Erro ao processar URL: " + e.getMessage());
            throw new RemoteException("Erro ao processar URL", e);
        }
    }

    /**
      * Organizar os dados de cada resultado da pesquisa: URL, título e citação
      */
    private static class ResultadoPesquisa {
        String url;
        String titulo;
        String citacao;

        public ResultadoPesquisa(String url, String titulo, String citacao) {
            this.url = url;
            this.titulo = titulo;
            this.citacao = citacao;
        }

        @Override
        public String toString() {
            return "Título: " + titulo + "\nURL: " + url + "\nCitação: " + citacao;
        }
    }


    //salvar as 10 pesquisas + frequentes em um arquivo, será chamado antes do servidor ser encerrado
    private void salvarPesquisasFrequentes() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoPesquisas))) {
            pesquisasFrequentes.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(entry -> {
                    try {
                        writer.write(entry.getKey() + ":" + entry.getValue() + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            System.out.println("Pesquisas frequentes salvas com sucesso.");
        } 
        catch (IOException e) {
            System.err.println("Erro ao salvar pesquisas frequentes: " + e.getMessage());
            e.printStackTrace();
        }
    }


    //carregar as pesquisas do arquivo para a memória quando o servidor iniciar
    private void carregarPesquisasFrequentes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(arquivoPesquisas))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] partes = linha.split(":");
                if (partes.length == 2) {
                    String palavra = partes[0];
                    int frequencia = Integer.parseInt(partes[1]);
                    pesquisasFrequentes.put(palavra, frequencia);
                }
            }
            System.out.println("Pesquisas frequentes carregadas com sucesso.");
        } catch (IOException e) {
            System.err.println("Erro ao carregar pesquisas frequentes: " + e.getMessage());
        }
    }

    /**
     * Realiza uma pesquisa distribuída nos Barrels.
     *
     * @param palavra palavra-chave da pesquisa.
     * @return uma lista de URLs resultantes da pesquisa.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    @Override
    public List<String> pesquisar(String palavra) throws RemoteException {

        // 1. Lida com pesquisa nula ou vazia
        if (palavra == null || palavra.trim().isEmpty()) {
            System.out.println("Pesquisa vazia recebida.");
            this.resultadosPesquisa = new ArrayList<>(); // Limpa resultados anteriores
            this.paginaAtual = 0;
            return new ArrayList<>(); // Retorna lista vazia
        }

        // 2. Atualiza a contagem da palavra pesquisada - estatísticas. */
        pesquisasFrequentes.put(palavra, pesquisasFrequentes.getOrDefault(palavra, 0) + 1);
        salvarPesquisasFrequentes(); // Salva após cada pesquisa

        // 3. Dividir a pesquisa em termos individuais (ignora múltiplos espaços, converte para minúsculas) e FILTRAR stopwords usando ehProvavelStopword
        String[] termosBrutos = palavra.toLowerCase().split("\\s+");
        List<String> termosValidos = new ArrayList<>();
        List<String> termosIgnorados = new ArrayList<>();

        for (String termo : termosBrutos) {
            String termoLimpo = termo.trim();
            if (!termoLimpo.isEmpty()) {
                // Chama ehProvavelStopword com valores dummy (0, 1)
                if (StopwordClassificador.ehProvavelStopword(termoLimpo, 0, 1)) {
                    termosIgnorados.add(termoLimpo);
                } else {
                    termosValidos.add(termoLimpo);
                }
            }
        }

        // Se a pesquisa só continha stopwords ou ficou vazia após filtrar
        if (termosValidos.isEmpty()) {
            System.out.println("Pesquisa '" + palavra + "' continha apenas stopwords ou ficou vazia (usando ehProvavelStopword). Termos ignorados: " + termosIgnorados);
            this.resultadosPesquisa = new ArrayList<>();
            this.paginaAtual = 0;
            return new ArrayList<>();
        }

        // 4. Buscar resultados para CADA termo em TODOS os barrels
        List<Set<String>> resultadosPorTermo = new ArrayList<>();
        for (String termoIndividual : termosValidos) {
            if (termoIndividual.isEmpty()) continue; // Pular strings vazias se houver espaços extras

            System.out.println("Buscando por termo: '" + termoIndividual + "'");
            Set<String> urlsParaTermoAtual = new HashSet<>();
            Map<String, Long> temposBarrelParaTermo = new HashMap<>(); // Rastrear tempo por barrel para este termo

            for (InterfaceBarrel barrel : barrels) {
                long inicio = System.nanoTime();
                try {
                    // 'termoIndividual' é a palavra individual que o barrel.pesquisar() espera
                    List<String> barrelResultados = barrel.pesquisar(termoIndividual);
                    if (barrelResultados != null) {
                        for (String url : barrelResultados) {
                            if (url != null && !url.isBlank()) {
                                urlsParaTermoAtual.add(normalizarURL(url));
                            }
                        }
                    }
                    long duracao = System.nanoTime() - inicio;
                    String nomeBarrel = barrel.getNomeBarrel();
                    temposBarrelParaTermo.put(nomeBarrel, duracao);

                } 
                catch (RemoteException e) {
                    System.err.println("Erro ao consultar o Barrel " + barrel.getNomeBarrel() + " para o termo '" + termoIndividual + "': " + e.getMessage());
                }
                catch (Exception e) {
                    System.err.println("Erro Barrel " + barrel.getNomeBarrel() + " para o termo '" + termoIndividual + "': " + e.getMessage());
                }
            }
            System.out.println("URLs encontradas para '" + termoIndividual + "': " + urlsParaTermoAtual.size());
            resultadosPorTermo.add(urlsParaTermoAtual);

            temposBarrelParaTermo.forEach((nomeBarrel, duracaoNano) -> {
                temposResposta.putIfAbsent(nomeBarrel, new ArrayList<>());
                temposResposta.get(nomeBarrel).add(duracaoNano / 100000); 
            });
        }

        // 5. Calcular a interseção dos resultados (URLs que contêm TODOS os termos)
        Set<String> urlsComuns = new HashSet<>();
        if (!resultadosPorTermo.isEmpty()) {
            urlsComuns.addAll(resultadosPorTermo.get(0)); // Começa com os resultados do primeiro termo
            for (int i = 1; i < resultadosPorTermo.size(); i++) {
                urlsComuns.retainAll(resultadosPorTermo.get(i));
                if (urlsComuns.isEmpty()) {
                    System.out.println("Interseção zerou após termo: '" + termosValidos.get(i) + "'");
                    break;
                }
            }
        }
        System.out.println("URLs comuns encontradas após interseção: " + urlsComuns.size());


        //DAQUI PARA BAIXO LIDA COM AS URLS COMUNS

        // Buscar URLs no Hacker News
    List<String> urlsHackerNews = buscarTopStoriesHackerNews(palavra);
    if (urlsHackerNews != null && !urlsHackerNews.isEmpty()) {
        System.out.println("URLs do Hacker News encontradas: " + urlsHackerNews);
        urlsComuns.addAll(urlsHackerNews);

        // Indexar URLs do Hacker News nos Barrels
        for (String url : urlsHackerNews) {
            for (InterfaceBarrel barrel : barrels) {
                try {
                    barrel.indexar_URL(palavra, url);
                } catch (RemoteException e) {
                    System.err.println("Erro ao indexar URL no Barrel: " + e.getMessage());
                }
            }
        }
    } else {
        System.out.println("Nenhuma URL relevante encontrada no Hacker News para a palavra: " + palavra);
    }

        // 7. Calcular "popularidade" (backlinks) para as URLs comuns
        Map<String, Integer> ligacoesPorUrl = new HashMap<>();
        for (String url : urlsComuns) {
            int totalLigacoes = 0;
            for (InterfaceBarrel barrel : barrels) {
                try {
                    List<String> apontadores = barrel.obterPaginasApontandoPara(url);
                    totalLigacoes += (apontadores != null ? apontadores.size() : 0);
                }
                catch (RemoteException e) {
                    System.err.println("Erro ao obter backlinks para " + url + " no barrel " + barrel.getNomeBarrel() + ": " + e.getMessage());
                }
            }
            ligacoesPorUrl.put(url, totalLigacoes);
        }

        // 8. Ordenar as URLs comuns pela popularidade (backlinks)
        List<String> resultadosOrdenados = new ArrayList<>(urlsComuns);
        resultadosOrdenados.sort((url1, url2) -> ligacoesPorUrl.getOrDefault(url2, 0) - ligacoesPorUrl.getOrDefault(url1, 0));
        
        // 9. Obter detalhes (Título, Citação) para as URLs ordenadas
        List<ResultadoPesquisa> resultadosComDetalhes = new ArrayList<>();
        for (String url : resultadosOrdenados) {
            try {
                ResultadoPesquisa resultado = obterDetalhesDaURL(url);
                if (resultado != null) {
                    resultadosComDetalhes.add(resultado);
                } else {
                    System.out.println("Não foi possível obter detalhes para: " + url);
                }
            } catch (Exception e) {
                System.err.println("Erro ao obter detalhes da URL: " + url + " - " + e.getMessage());
            }
        }

        // 10. Preparar para paginação e retorno
        this.resultadosPesquisa = resultadosComDetalhes;
        this.paginaAtual = 0;
        System.out.println("Total de resultados finais com detalhes: " + this.resultadosPesquisa.size());

        List<String> resultadosFormatadosPrimeiraPagina = new ArrayList<>();
        for (ResultadoPesquisa res : getResultadosPaginaAtual()) {
            resultadosFormatadosPrimeiraPagina.add(res.toString());
        }
        return resultadosFormatadosPrimeiraPagina;
    }

    /** Método auxiliar para normalizar URLs para deduplicação mais eficaz */
    private String normalizarURL(String url) {
        if (url == null) return "";
        url = url.trim().toLowerCase();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Extrai o título e uma citação curta de uma página da web.
     *
     * @param url URL da página.
     * @return um objeto ResultadoPesquisa contendo a URL, o título e a citação, ou null em caso de erro.
     */
    private ResultadoPesquisa obterDetalhesDaURL(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            String titulo = doc.title();
            Element primeiroParagrafo = doc.select("p").first();
            String citacao = primeiroParagrafo != null ? primeiroParagrafo.text() : "Sem citação disponível.";
            return new ResultadoPesquisa(url, titulo, citacao);
        } 
        catch (IOException e) {
            System.err.println("Erro ao obter detalhes da URL: " + url + " - " + e.getMessage());
            return null;
        }
    }


    /**
     * Avança para a próxima página de resultados da pesquisa, se disponível.
     *
     * @return uma mensagem indicando que não há mais resultados ou uma string contendo os resultados da próxima página.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    @Override
    public String next_page() throws RemoteException {
        if (resultadosPesquisa == null || resultadosPesquisa.isEmpty()) {
            return "Nenhum resultado disponível.";
        }
        if ((paginaAtual + 1) * TAMANHO_PAGINA < resultadosPesquisa.size()) {
            paginaAtual++;
        }
        return formatarResultadosDaPagina();
    }

    /**
     * Retorna para a página anterior de resultados da pesquisa, se disponível.
     *
     * @return string contendo os resultados da página anterior.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    @Override
    public String previous_page() throws RemoteException {
        if (paginaAtual > 0) {
            paginaAtual--;
        }
        return formatarResultadosDaPagina();
    }

    /**
     * Obtém os links da página atual de resultados da pesquisa.
     *
     * @return lista contendo os links da página atual.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    @Override
    public List<String> links_to_page() throws RemoteException {
        List<String> links = new ArrayList<>();
         for (ResultadoPesquisa resultado : getResultadosPaginaAtual()) {
             links.add(resultado.url);
         }
         return links;
    }

    /**
     * Obtém os resultados da página atual com base no índice da página.
     *
     * @return sublista dos resultados da pesquisa, correspondente à página atual.
     */
    private List<ResultadoPesquisa> getResultadosPaginaAtual() {
        if (resultadosPesquisa == null || resultadosPesquisa.isEmpty()) {
            return new ArrayList<>();
        }

            // Garantir que paginaAtual esteja dentro do intervalo válido
        int totalPaginas = (int) Math.ceil((double) resultadosPesquisa.size() / TAMANHO_PAGINA);
        if (paginaAtual >= totalPaginas) {
            paginaAtual = totalPaginas - 1; // Ajustar para a última página válida
        } else if (paginaAtual < 0) {
            paginaAtual = 0; // Ajustar para a primeira página
        }


        int inicio = paginaAtual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, resultadosPesquisa.size());

            // Verificar se os índices são válidos
        if (inicio >= resultadosPesquisa.size() || inicio < 0 || fim < 0 || fim > resultadosPesquisa.size()) {
            System.err.println("Índices inválidos: início=" + inicio + ", fim=" + fim);
            return new ArrayList<>();
        }

        System.out.println("Página atual: " + paginaAtual);
        System.out.println("Total de resultados: " + resultadosPesquisa.size());
        return resultadosPesquisa.subList(inicio, fim);
    }

    private String formatarResultadosDaPagina() {
        StringBuilder sb = new StringBuilder();
        for (ResultadoPesquisa resultado : getResultadosPaginaAtual()) {
            sb.append(resultado.toString()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Gera um relatório contendo estatísticas do sistema, incluindo:
     * - As 10 pesquisas mais frequentes.
     * - Lista de barrels ativos e o tamanho de seus índices.
     * - Tempo médio de resposta de cada barrel.
     * @return string formatada contendo o relatório de estatísticas.
     * @throws RemoteException  -> caso ocorrer um erro de comunicação remota.
     */
    @Override
    public String pagina_estatisticas() throws RemoteException {
        StringBuilder relatorioEstatisticas = new StringBuilder();
    
        relatorioEstatisticas.append("---- ESTATÍSTICAS ----\n");
    
        gerarRelatorioPesquisasFrequentes(relatorioEstatisticas);
        gerarRelatorioBarrelsAtivos(relatorioEstatisticas);
        gerarRelatorioTemposResposta(relatorioEstatisticas);
    
        return relatorioEstatisticas.toString();     // retorna o relatório final como string
    }

    /** Lista das 10 pesquisas mais frequentes */
    private void gerarRelatorioPesquisasFrequentes(StringBuilder relatorio) {
        /** Visualiza o conteúdo do mapa pesquisasFrequentes e mostra no terminal do servidor. */
        System.out.println("Pesquisas frequentes: " + pesquisasFrequentes); 
        
        /** append -> adiciona texto na stringbuilder, nesse caso um título para o relatório */
        relatorio.append("Top 10 pesquisas mais comuns:\n");
    
        pesquisasFrequentes.entrySet().stream()                             // converte pesquisasFrequentes (palavra -> quantidade) em um fluxo de dados.
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))         // ordena os resultados do maior para o menor número de pesquisas.
            .limit(10)                                              // mantém só os 10 + pesquisados
            .forEach(entry -> relatorio.append(entry.getKey())  // para cada entrada, adiciona a palavra pesquisada
                                .append(": ")          // adiciona um separador
                                .append(entry.getValue())  // adiciona o número de vezes que foi pesquisada
                                .append("\n"));        // pula para a próxima linha
    }


    /** Lista de Barrels ativos e tamanhos do índice */
    private void gerarRelatorioBarrelsAtivos(StringBuilder relatorio) {
        System.out.println("Barrels ativos: " + barrelsAtivos); 
        
        relatorio.append("\nBarrels ativos e tamanho do índice:\n");
    
        barrelsAtivos.forEach((barrel, tamanho) ->        // percorre barrelsAtivos (Barrel -> tamanho do índice).
        relatorio.append(barrel)              // adiciona o nome do Barrel.
                .append(": ")                         
                .append(tamanho)                          // adiciona o tamanho do índice (número de URLs indexados).
                .append(" URLs\n"));                  // especifica que o valor representa URLs e pula para a próxima linha.
    }
    

    /** Lista de tempos de resposta dos barrels */
    private void gerarRelatorioTemposResposta(StringBuilder relatorio) {
        System.out.println("Tempos de resposta por Barrel: " + temposResposta); 

        relatorio.append("\nTempo médio de resposta por Barrel (nanossegundos):\n");
    
        for (var entry : temposResposta.entrySet()) {      // percorre temposResposta (Barrel -> lista de tempos de resposta).
            String barrel = entry.getKey();                // obtém o nome do Barrel.
            List<Long> tempos = entry.getValue();          // obtém a lista de tempos de resposta para esse Barrel.
        
            /** Verifica se há tempos registrados para o Barrel. */
            if (tempos.isEmpty()) {
                relatorio.append(barrel).append(": Sem dados\n");    // exibe "Sem dados" caso não haja tempos.
            } 
            else {
                long soma = 0;
                for (Long tempo : tempos) {
                    soma += tempo;
                }
                double media = (double) soma / tempos.size();  // Calcula a média em nanossegundos

                relatorio.append(barrel)           
                    .append(": ")                          
                    .append(String.format("%.2f", media))  // Adiciona o tempo médio de resposta formatado
                    .append(" nanossegundos\n");   
            }                      
        }
    }

    /**
     * Sempre que indexar algo no Barrel é atualizado o tamanho.
     *
     * @param barrel nome do barrel cujo tamanho será atualizado.
     * @param tamanho novo tamanho do índice do barrel.
     */
    public void atualizarTamanhoBarrel(String barrel, int tamanho) {
        barrelsAtivos.put(barrel, tamanho);
    }

    //carrega a lista de URLs previamente indexadas do urlsIndexados p/ memória, pra que possam ser usadas pelo programa
    private void carregarURLs() {
        try (BufferedReader br = new BufferedReader(new FileReader(ArquivoURLS))) {  //essa linha permite acessar o texto dentro do arquivo --- por causa do readLine, o BufferedReader busca caracteres no buffer até encontrar uma quebra de linha e entrega de volta a linha inteira como uma String
            String url;
            while ((url = br.readLine()) != null) {    //br.readline é usado pra ler o arquivo linha por linha - cada linha = 1 url
                urlsIndexados.add(url);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar URLs indexadas: " + e.getMessage());
        }
    }

    
     /**
     * Busca as "top stories" do Hacker News e retorna URLs que contenham o termo de pesquisa no título ou URL.
     *
     * @param termo Termo de pesquisa a ser usado para filtrar as histórias.
     * @return Lista de URLs das histórias que correspondem ao termo.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<String> buscarTopStoriesHackerNews(String termo) throws RemoteException {
        List<String> urlsEncontradas = new ArrayList<>();
    try {
        // Endpoint para obter IDs das top stories
        String topStoriesEndpoint = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";

        // Obter os IDs das histórias principais
        RestTemplate restTemplate = new RestTemplate();
        List<Integer> topStoriesIds = restTemplate.getForObject(topStoriesEndpoint, List.class);

        if (topStoriesIds == null || topStoriesIds.isEmpty()) {
            System.err.println("Nenhuma história encontrada no Hacker News.");
            return urlsEncontradas;
        }

        System.out.println("IDs das top stories do Hacker News: " + topStoriesIds);

        // Iterar sobre os primeiros 50 IDs (ou menos, se houver menos histórias)
        for (int i = 0; i < Math.min(topStoriesIds.size(), 50); i++) {
            Integer storyId = topStoriesIds.get(i);

            // Endpoint para obter detalhes de cada história
            String storyDetailsEndpoint = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json?print=pretty", storyId);
            HackerNewsItemRecord storyDetails = restTemplate.getForObject(storyDetailsEndpoint, HackerNewsItemRecord.class);

            if (storyDetails == null || storyDetails.url() == null || storyDetails.title() == null) {
                System.err.println("Detalhes da história " + storyId + " estão incompletos ou nulos.");
                continue;
            }

            System.out.println("Detalhes da história " + storyId + ": " + storyDetails);

            // Verificar se o termo está no título ou URL
            if (termo == null || termo.isBlank() || 
                storyDetails.title().toLowerCase().contains(termo.toLowerCase()) || 
                storyDetails.url().toLowerCase().contains(termo.toLowerCase())) {
                urlsEncontradas.add(storyDetails.url());
            }
        }
    } catch (Exception e) {
        System.err.println("Erro ao buscar top stories do Hacker News: " + e.getMessage());
    }
    return urlsEncontradas;
    }

    @Override
    public List<String> obterPesquisasMaisFrequentes() throws RemoteException {
        List<String> pesquisas = new ArrayList<>();
        pesquisasFrequentes.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .forEach(entry -> pesquisas.add(entry.getKey() + ": " + entry.getValue()));
        return pesquisas;
    }
    
    @Override
    public Map<String, Integer> obterBarrelsAtivos() throws RemoteException {
        return new HashMap<>(barrelsAtivos);
    }
    
    @Override
    public Map<String, Double> obterTemposResposta() throws RemoteException {
        Map<String, Double> temposMedios = new HashMap<>();

        for (var entry : temposResposta.entrySet()) {
            String barrel = entry.getKey();
            List<Long> tempos = entry.getValue();
            
            if (tempos.isEmpty()) {
                temposMedios.put(barrel, 0.0); // Sem dados, retorna 0.0
            } else {
                long soma = 0;
                for (Long tempo : tempos) {
                    soma += tempo;
                }
                double media = (double) soma / tempos.size(); // Média em nanossegundos
                temposMedios.put(barrel, media);
            }
        }
        return temposMedios;
    }
    @Override
    public String gerarAnaliseContextualizada(String termo) {
        try {
            // Delegar a geração da análise para o OpenRouterApiService
            OpenRouterApi openRouterApiService = new OpenRouterApi();
            return openRouterApiService.gerarAnaliseContextualizada(termo);
        } catch (Exception e) {
            System.err.println("Erro ao gerar análise contextualizada: " + e.getMessage());
            return "Não foi possível gerar a análise contextualizada.";
        }
 }
    @Override
    public List<String> consultarRelacoes(String url) throws RemoteException {
        Set<String> urlsRelacionadas = new HashSet<>(); // Usar Set para evitar URLs duplicadas
        if (url == null || url.trim().isEmpty() || !relacoesDeUrls.containsKey(url)) {
            return new ArrayList<>(urlsRelacionadas); // Retorna lista vazia se a URL base for inválida ou não estiver no mapa
        }

        // Obtém a lista de palavras-chave que estão associadas à url.
        // urlsIndexados.txt tem "java -> http://example.com/java"
        // então para url="http://example.com/java", palavrasChaveDaUrlBase será ["java", "programming"]
        List<String> palavrasChaveDaUrlBase = relacoesDeUrls.get(url);

        if (palavrasChaveDaUrlBase == null || palavrasChaveDaUrlBase.isEmpty()) {
            return new ArrayList<>(urlsRelacionadas);                               // Nenhuma palavra-chave associada à URL base, então não há URLs relacionadas por este critério.
        }

        // Agora, para cada URL candidata no mapa relacoesDeUrls,
        // verifique se alguma das palavras-chave que apontam para a url também aponta para a URL candidata.
        for (Map.Entry<String, List<String>> entry : relacoesDeUrls.entrySet()) {
            String urlCandidata = entry.getKey();
            List<String> palavrasChaveDaUrlCandidata = entry.getValue();

            if (urlCandidata.equals(url)) {
                continue; // Não relacionar uma URL consigo mesma
            }

            if (palavrasChaveDaUrlCandidata == null) {
                continue;
            }

            // Verifica se há alguma palavra-chave em comum
            for (String palavraChaveDaUrlOriginal : palavrasChaveDaUrlBase) {
                if (palavrasChaveDaUrlCandidata.contains(palavraChaveDaUrlOriginal)) {
                    urlsRelacionadas.add(urlCandidata); // Adiciona a URL candidata relacionada
                    break; // Encontrou uma palavra-chave comum, passa para a próxima urlCandidata
                }
            }
        }
        return new ArrayList<>(urlsRelacionadas); // Retorna a lista de URLs relacionadas
    }   
}