import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GatewayServer extends UnicastRemoteObject implements InterfaceGatewayServer {
    //precisa do -extends UnicastRemoteObject- pois ele faz automaticamente a exportação dos objetos remotos para que os clientes
    //consigam chamá-lo remotamente
    
    private List<InterfaceBarrel> barrels;
    private static final String[] palavras_chave = {""}; // Definir palavras chave aqui
    private static final String ArquivoURLS = "urlsIndexados.txt";
    private List<String> urlsIndexados;

    //estruturas necessárias p/ armazenar as estatísticas
    private final Map<String, Integer> pesquisasFrequentes;  //contador das pesquisas + comuns
    private final Map<String, Integer> barrelsAtivos;        //lista de Barrels ativos e tamanhos de índice
    private final Map<String, List<Long>> temposResposta;    //tempo médio de resposta por Barrel

    //estruturas necessárias p nextpage, previous e link
    private List<String> resultadosPesquisa;                 //resultados da última pesquisa feita - list pois não precisa estar associada a uma chave, são só urls
    private int paginaAtual;                                 //indice da pág atual
    private static final int TAMANHO_PAGINA = 10;            //nº de resultados por pág

    protected GatewayServer(String serverIp) throws RemoteException {    //protegido para garantir que só classes filhas ou dentro do mesmo
        // pacote possam instanciar o objeto diretamente
        super();              //exporta o objeto remoto automaticamente, sem isso, o objeto não ficaria disponível para chamadas remotas
        this.barrels = new ArrayList<>();
        this.urlsIndexados = new ArrayList<>();         //inicializa a lista de URLs
        //inicia os maps das estatísticas
        this.pesquisasFrequentes = new HashMap<>();     
        this.barrelsAtivos = new HashMap<>();
        this.temposResposta = new HashMap<>();
        this.resultadosPesquisa = new ArrayList<>();
        this.paginaAtual = 0;                           //pags começam com 0

        // Lista de barrels disponíveis
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
                // Carregar propriedades do arquivo
                Properties properties = new Properties();
                try (InputStream input = new FileInputStream("config.properties")) {
                    properties.load(input);
                }
    
                // Obter o IP do servidor a partir das propriedades
                String serverIp = properties.getProperty("server.ip", "localhost");
                String objName = "rmi://" + serverIp + "/server";
    
                GatewayServer server = new GatewayServer(serverIp);   // Instancia o servidor
    
                System.out.println("Registrando objeto no RMIRegistry...");
    
                try {
                    LocateRegistry.createRegistry(1099);  // Cria um registry RMI na porta 1099
                    System.out.println("Registry RMI criado na porta 1099.");
                } catch (Exception e) {
                    System.out.println("Registry RMI já existente.");
                }
    
                Naming.rebind(objName, server);  // Registra o objeto remoto no RMI Registry
    
                System.out.println("Servidor RMI pronto...");
            } catch (Exception e) {
                e.printStackTrace();  // Caso dê erro, ele será impresso
            }
        }


    private void conectarBarrels(List<String> barrelUrls) {

        barrels.clear();    

        for(String barrelUrl: barrelUrls){
            try { 
                    //conecta barrels usando a URL fornceida
                    InterfaceBarrel barrel = (InterfaceBarrel) Naming.lookup(barrelUrl);
                    barrels.add(barrel);       //add o barrel conectado à lista de barrels
                    System.out.println("Conectado ao barrel: " + barrelUrl);
                
            } catch (Exception e) {
                System.err.println("Erro ao conectar aos barrels " + barrelUrl);
                e.printStackTrace();
            }
        }
    }

    //MÉTODO PRA colocar a url na fila
    @Override
    public void enviarURLParaProcessamento(String url) throws RemoteException {
        if (barrels.isEmpty()) {
            System.out.println("Nenhum Barrel disponível para processar a URL.");
            return;
        }

        //escolher Barrel com menos URLs na fila
        InterfaceBarrel melhorBarrel = null;
        int menorFila = Integer.MAX_VALUE;

        for (InterfaceBarrel barrel : barrels) {
            try {
                int tamanhoFila = barrel.tamanhoFilaURLs(); // Obtém o tamanho da fila de cada Barrel
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
            melhorBarrel.adicionarURLNaFila(url);
            System.out.println("URL enviada para processamento no Barrel.");
        } 
        else {
            System.out.println("Nenhum Barrel disponível para receber a URL.");
        }
    }

    //MONITORAMENTO DO BARRELS
    private void iniciarMonitoramento() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Verifica a cada 5 segundos
                    //System.out.println("Monitorando Barrels...");

                    // Limpa apenas os barrels inativos do mapa
                    barrelsAtivos.entrySet().removeIf(entry -> !barrelAindaAtivo(entry.getKey()));
                    for (InterfaceBarrel barrel : barrels) {
                        try {
                            if (barrel.estaAtivo()) {               //verifica se cada barrel está ativo
                                String nomeBarrel = barrel.getNomeBarrel();
                                int tamanhoIndice = barrel.getTamanhoIndice();        //pega o total INDEXADO
                                atualizarTamanhoBarrel(nomeBarrel, tamanhoIndice);
                            }
                        } catch (RemoteException e) {
                            System.err.println("Barrel inativo detectado!");  //quando o barrel está inativo manda aviso de erro
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();                //isso é o que realmente faz a thread começar a rodar
    }


    //Método pra pegaros barrels que estão ativos
    private boolean barrelAindaAtivo(String nome) {
        for (InterfaceBarrel barrel : barrels) {
            if (barrel.toString().equals(nome)) {
                return true;
            }
        }
        return false;
    }

    // RELIABLE MULTICAST
    public boolean reliableMulticast(String palavra, String url) {    //garante que os dados estao replicadso entre os barrels
        List<InterfaceBarrel> confirmados = new ArrayList<>();

        try {
            //enviar para todos os barrels
            for (InterfaceBarrel barrel : barrels) {
                barrel.indexar_URL(palavra, url);
            }

            //confirmar recebimento
            for (InterfaceBarrel barrel : barrels) {
                if (barrel.confirmarRecebimento(palavra, url)) {
                    confirmados.add(barrel);
                } else {
                    System.err.println("Erro: Barrel não confirmou recebimento.");    //se qualquer barrel nao confirmar, a operação falha
                    return false;
                }
            }

            System.out.println("Indexação concluída com sucesso!");    //quando todos confirmam, é sucesso
            return true;
        } catch (RemoteException e) {
            System.err.println("Falha no Reliable Multicast: " + e.getMessage());
            return false;
        }
    }

    // INDEXAR URL = salvar
    @Override
    public void indexar_URL(String url) throws RemoteException {
        if (!urlsIndexados.contains(url)) {
            
            //distribui a indexação para os barrels
            for (InterfaceBarrel barrel : barrels) {
                for (String palavra : palavras_chave) {
                    barrel.indexar_URL(palavra, url);      //indexar a URL em cada barrel
                }
            }

            urlsIndexados.add(url);
            System.out.println("URL indexada em todos os barrels: " + url);
        } 
        else {
            System.out.println("URL já foi indexado.");
        }
    }

    // PESQUISAR
    @Override
    public List<String> pesquisar(String palavra) throws RemoteException {
        List<String> resultados = new ArrayList<>();

        //consultar cada barrel e combina os resultados
        for (InterfaceBarrel barrel : barrels) {
            long inicio = System.nanoTime();
            
            List<String> barrelResultados = barrel.pesquisar(palavra);
            resultados.addAll(barrelResultados);

            long duracao = (System.nanoTime() - inicio) / 100000;   //calcula tempo para este Barrel

            //atualização de tempo de resposta no mapa por Barrel 
            String nomeBarrel = barrel.getNomeBarrel();            //pega o nome real do Barrel usado
            temposResposta.putIfAbsent(nomeBarrel, new ArrayList<>());
            temposResposta.get(nomeBarrel).add(duracao);
        }

        //atualiza a contagem da palavra pesquisada - ESTATÍSTICAS
        pesquisasFrequentes.put(palavra, pesquisasFrequentes.getOrDefault(palavra, 0) + 1);

        this.resultadosPesquisa = resultados;
        this.paginaAtual = 0;

        return resultados;
    }

    @Override
    public String next_page() throws RemoteException {
        if (resultadosPesquisa.isEmpty()) {
            return "Nenhum resultado disponível.";
        }
        if ((paginaAtual + 1) * TAMANHO_PAGINA < resultadosPesquisa.size()) {
            paginaAtual++;
        }
        return String.join("\n", getResultadosPaginaAtual());
    }

    @Override
    public String previous_page() throws RemoteException {
        if (paginaAtual > 0) {
            paginaAtual--;
        }
        return String.join("\n", getResultadosPaginaAtual());
    }

    @Override
    public List<String> links_to_page() throws RemoteException {
        return getResultadosPaginaAtual();
    }

    private List<String> getResultadosPaginaAtual() {
        int inicio = paginaAtual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, resultadosPesquisa.size());
        return resultadosPesquisa.subList(inicio, fim);
    }

    @Override
    public String pagina_estatisticas() throws RemoteException {
        StringBuilder relatorioEstatisticas = new StringBuilder();  //cria objeto stringbuilder

        relatorioEstatisticas.append("---- ESTATÍSTICAS ----\n");
        
        //vê o conteúdo do mapa pesquisasFrequentes e printa no TERMINAL do servidor
        System.out.println("Pesquisas frequentes: " + pesquisasFrequentes);
        
        //TOP 10 PESQUISAS + comuns
        relatorioEstatisticas.append("Top 10 pesquisas mais comuns:\n");     //append -> add texto na stringbuilder, nesse caso um título pro nosso relatório
 
        pesquisasFrequentes.entrySet().stream()                             //converte pesquisasFrequentes (palavra -> quantidade) em um fluxo de dados
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))         //ordena os resultados do maior para o menor número de pesquisas
            .limit(10)                                              //mantém só os 10 + pesquisados
            .forEach(entry -> relatorioEstatisticas.append(entry.getKey())  //p/ cada entrada, adiciona a palavra pesquisada
                                .append(": ")          //add um separador
                                .append(entry.getValue())  //add nº de vezes que foi pesquisada
                                .append("\n"));        //pula para a próxima linha

        

        //vê o conteúdo do mapa barrelsAtivos e printa no TERMINAL do servidor
        System.out.println("Barrels ativos: " + barrelsAtivos);
        
        //LISTA DE BARRELS ATIVOS e tamanhos do índice
        relatorioEstatisticas.append("\nBarrels ativos e tamanho do índice:\n");                          //titulo

        barrelsAtivos.forEach((barrel, tamanho) ->        //percorre barrelsAtivos (Barrel -> tamanho do índice)
        relatorioEstatisticas.append(barrel)              //add o nome do Barrel
                .append(": ")                         
                .append(tamanho)                          //add o tamanho do índice (número de URLs indexados)
                .append(" URLs\n"));                  //especifica que o valor representa URLs e pula para a próxima linha


        
        //vê o conteúdo do mapa temposResposta e printa no TERMINAL do servidor
        System.out.println("Tempos de resposta por Barrel: " + temposResposta);

        //TEMPO MÉDIO DE RESPOSTA DOS BARRELS
        relatorioEstatisticas.append("\nTempo médio de resposta por Barrel (décimos de segundo):\n");      //titulo
        
        for (var entry : temposResposta.entrySet()) {      //percorre temposResposta (Barrel -> lista de tempos de resposta)
            String barrel = entry.getKey();                //pega o nome do Barrel
            List<Long> tempos = entry.getValue();          //pega a lista de tempos de resposta para esse Barrel
            
            // Verifica se há tempos registrados para o Barrel
            if (tempos.isEmpty()) {
                relatorioEstatisticas.append(barrel).append(": Sem dados\n");                             // Exibe "Sem dados" caso não haja tempos
            } 
            else {
                long media = tempos.stream()                   //converte a lista de tempos de resposta em um fluxo de dados
                            .mapToLong(Long::longValue)        //converte a lista de objetos Long para primitivos long
                            .sum() / tempos.size();            //calcula a média somando todos os valores e dividindo pelo total

                // Converte para microsegundos dividindo por 1000 (porque estamos em nanossegundos no cálculo de tempo)
                long mediaEmMicrosegundos = media / 1000;

                relatorioEstatisticas.append(barrel)           
                    .append(": ")                          
                    .append(mediaEmMicrosegundos)                        //add o tempo médio de resposta
                    .append("\n");   
            }                      
        }

        return relatorioEstatisticas.toString();           //retorna o relatório final como string
    }

    //sempre que indexar algo no barrel ele atualiza o tamanho
    public void atualizarTamanhoBarrel(String barrel, int tamanho) {
        barrelsAtivos.put(barrel, tamanho);
    }

    private void carregarURLs() {
        try (BufferedReader br = new BufferedReader(new FileReader(ArquivoURLS))) {
            String url;
            while ((url = br.readLine()) != null) {
                urlsIndexados.add(url);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar URLs indexadas: " + e.getMessage());
        }
    }

}
