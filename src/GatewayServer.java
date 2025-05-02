import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.rmi.registry.LocateRegistry;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * GatewayServer é responsável por gerenciar a comunicação entre os clientes e os servidores Barrel.
 * Distribui URLs para processamento, realiza buscas distribuídas, monitora a atividade dos Barrels e mantém estatísticas de uso.
 * Essa classe implementa a interface InterfaceGatewayServer e estende UnicastRemoteObject, permitindo chamadas remotas via remota.
 */

public class GatewayServer extends UnicastRemoteObject implements InterfaceGatewayServer {
// NOTE: é necessário o -extends UnicastRemoteObject- pois ele faz automaticamente a exportação dos objetos remotos para que os clientes consigam chamá-lo remotamente.
    
    /** Lista de Barrels conectados ao Gateway. */
    private List<InterfaceBarrel> barrels;

    /** Lista de palavras-chave utilizadas para indexação. */
    private static final String[] palavras_chave = {""};

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

    /**
     * Construtor da classe GatewayServer.
     * Inicializa as estruturas de dados e estabelece a conexão com os Barrels disponíveis.
     * Protegido para garantir que só classes filhas ou dentro do mesmo pacote possam instanciar o objeto diretamente.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    protected GatewayServer(String serverIp) throws RemoteException {    
        super();              // NOTE: exporta o objeto remoto automaticamente, sem isso, o objeto não ficaria disponível para chamadas remotas.
        this.barrels = new ArrayList<>();
        this.urlsIndexados = new ArrayList<>();    
        this.pesquisasFrequentes = new HashMap<>();     
        this.barrelsAtivos = new HashMap<>();
        this.temposResposta = new HashMap<>();
        this.resultadosPesquisa = new ArrayList<>();
        this.paginaAtual = 0;                           

        /** Lista de Barrels disponíveis para conexão. */
        List<String> barrelUrls = Arrays.asList(
            "rmi://" + serverIp + "/barrel1",
            "rmi://" + serverIp + "/barrel2",
            "rmi://" + serverIp + "/barrel3"
        );

        conectarBarrels(barrelUrls);
        carregarURLs();
        iniciarMonitoramento();

    }
        public static void main(String[] args) {
            try {
                /** Carrega propriedades do arquivo. */
                Properties properties = new Properties();
                try (InputStream input = new FileInputStream("config.properties")) {
                    properties.load(input);
                }
    
                /** Obtém o IP do servidor a partir das propriedades. */
                String serverIp = properties.getProperty("server.ip", "localhost");
                String objName = "rmi://" + serverIp + "/server";
    
                GatewayServer server = new GatewayServer(serverIp);
    
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
            } catch (Exception e) {
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
                
            } catch (Exception e) {
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
        if (!urlsIndexados.contains(url)) {
            
            /** Distribui a indexação para os Barrels. */
            for (InterfaceBarrel barrel : barrels) {
                for (String palavra : palavras_chave) {
                    barrel.indexar_URL(palavra, url);      // indexar a URL em cada Barrel.
                }
            }

            urlsIndexados.add(url);
            System.out.println("URL indexada em todos os barrels: " + url);
        } 
        else {
            System.out.println("URL já foi indexado.");
        }
    }

    /**
      * Classe interna para representar um resultado de pesquisa, contendo URL, título e citação.
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

    /**
     * Realiza uma pesquisa distribuída nos Barrels.
     *
     * @param palavra palavra-chave da pesquisa.
     * @return uma lista de URLs resultantes da pesquisa.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    @Override
    public List<String> pesquisar(String palavra) throws RemoteException {
        List<String> resultados = new ArrayList<>();
        List<ResultadoPesquisa> resultadosComDetalhes = new ArrayList<>();

        /** Consultar cada Barrel e combinar os resultados. */
        for (InterfaceBarrel barrel : barrels) {
            long inicio = System.nanoTime();    //tempo em nanossegundos (1 segundo = 1.000.000.000 nanossegundos)
            
            List<String> barrelResultados = barrel.pesquisar(palavra);
            resultados.addAll(barrelResultados);

            long duracao = (System.nanoTime() - inicio) / 100000; //dividindo por 100.000 transformaria p 10 microsegundos depois no relatorio eu divido por mais 1000 pra virar decimos de segundo

            /** Atualização de tempo de resposta no mapa por Barrel. */
            String nomeBarrel = barrel.getNomeBarrel();            // obtém o nome real do Barrel usado.
            temposResposta.putIfAbsent(nomeBarrel, new ArrayList<>());
            temposResposta.get(nomeBarrel).add(duracao);
        }

        for (String url : resultados) {
            ResultadoPesquisa resultado = obterDetalhesDaURL(url);
            if (resultado != null) {
                resultadosComDetalhes.add(resultado);
            }
        }

        /** Atualiza a contagem da palavra pesquisada - estatísticas. */
        pesquisasFrequentes.put(palavra, pesquisasFrequentes.getOrDefault(palavra, 0) + 1);

        this.resultadosPesquisa = resultadosComDetalhes;
        this.paginaAtual = 0;
        
        // Converter para List<String> para manter a compatibilidade com a assinatura do método
        List<String> resultadosFormatados = new ArrayList<>();
        for (ResultadoPesquisa resultado : resultadosPesquisa) {
            resultadosFormatados.add(resultado.toString());
        }
 
        return resultadosFormatados;
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
        if (resultadosPesquisa == null) {
            return new ArrayList<>();
        }

        int inicio = paginaAtual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, resultadosPesquisa.size());
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

        /** Visualiza o conteúdo do mapa pesquisasFrequentes e mostra no terminal do servidor. */
        System.out.println("Pesquisas frequentes: " + pesquisasFrequentes);

         /** append -> adiciona texto na stringbuilder, nesse caso um título para o relatório. */
        relatorioEstatisticas.append("Top 10 pesquisas mais comuns:\n");     
 
        pesquisasFrequentes.entrySet().stream()                             // converte pesquisasFrequentes (palavra -> quantidade) em um fluxo de dados.
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))         // ordena os resultados do maior para o menor número de pesquisas.
            .limit(10)                                              // mantém só os 10 + pesquisados.
            .forEach(entry -> relatorioEstatisticas.append(entry.getKey())  // para cada entrada, adiciona a palavra pesquisada.
                                .append(": ")          // adiciona um separador.
                                .append(entry.getValue())  // adiciona o número de vezes que foi pesquisada.
                                .append("\n"));        // pula para a próxima linha.

        System.out.println("Barrels ativos: " + barrelsAtivos);
    
        /** Lista de Barrels ativos e tamanhos do índice. */
        relatorioEstatisticas.append("\nBarrels ativos e tamanho do índice:\n");                          

        barrelsAtivos.forEach((barrel, tamanho) ->        // percorre barrelsAtivos (Barrel -> tamanho do índice).
        relatorioEstatisticas.append(barrel)              // adiciona o nome do Barrel.
                .append(": ")                         
                .append(tamanho)                          // adiciona o tamanho do índice (número de URLs indexados).
                .append(" URLs\n"));                  // especifica que o valor representa URLs e pula para a próxima linha.

        System.out.println("Tempos de resposta por Barrel: " + temposResposta);

        relatorioEstatisticas.append("\nTempo médio de resposta por Barrel (décimos de segundo):\n");  
        
        for (var entry : temposResposta.entrySet()) {      // percorre temposResposta (Barrel -> lista de tempos de resposta).
            String barrel = entry.getKey();                // obtém o nome do Barrel.
            List<Long> tempos = entry.getValue();          // obtém a lista de tempos de resposta para esse Barrel.
        
            /** Verifica se há tempos registrados para o Barrel. */
            if (tempos.isEmpty()) {
                relatorioEstatisticas.append(barrel).append(": Sem dados\n");    // exibe "Sem dados" caso não haja tempos.
            } 
            else {
                long media = tempos.stream()                   // converte a lista de tempos de resposta em um fluxo de dados.
                            .mapToLong(Long::longValue)        // converte a lista de objetos Long para primitivos long - conversão necessária pq ArrayList<Long> armazena objetos Long, não primitivos long e funções matemáticas trabalham com tipos primitivos p/ + eficiência
                            .sum() / tempos.size();            // calcula a média somando todos os valores e dividindo pelo total.

                /** Converte para microsegundos dividindo por 1000 (porque estamos em nanossegundos no cálculo de tempo). */
                long mediaEmMicrosegundos = media / 1000;

                relatorioEstatisticas.append(barrel)           
                    .append(": ")                          
                    .append(mediaEmMicrosegundos)          // adiciona o tempo médio de resposta.
                    .append("\n");   
            }                      
        }

        return relatorioEstatisticas.toString();           // retorna o relatório final como string.
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
}