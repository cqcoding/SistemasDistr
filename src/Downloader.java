import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;


/*Classe responsável por consumir URLs da fila de um Barrel remoto, baixar o conteúdo, extrair palavras, 
encontrar novos links (e enviá-los de volta ao Barrel) e indexar palavras em um servidor Barrel via RMI, usando múltiplas threads
*/
public class Downloader{
    //InterfaceBarrel barrel;     //conexão RMI com barrel
    private List<InterfaceBarrel> barrels; // Conexões RMI com múltiplos barrels
    private AtomicInteger barrelAtualIndex; // Índice para ciclo entre barrels ao obter URL
   
    private Set<String> urlsProcessadas;    //URLs já processadas/visitadas pelo downloader
    private Set<String> palavrasProcessadas;   //pares palavra_url já enviados
    private ConcurrentMap<String, AtomicInteger> contagemPalavras;
    private Set<String> stopWords;
    private final int numThreads;  //número de threads a serem usadas
    
    private ExecutorService executorService; // Pool de threads para processamento

    /** Limite de páginas para evitar loops infinitos (aplicado a este Downloader). */
    private static final int MAX_PAGES = 10;     //limite por instância do Downloader
    private final AtomicInteger contagemPaginas = new AtomicInteger(0);     //contador de páginas processadas 

    private final Object fileWriteLock = new Object();     //lock para sincronizar escrita no arquivo
    
    private final Random random = new Random(); // Instância Random para selecionar barrels para enviar dados

    /**
     * Construtor da classe Downloader.
     * Inicializa as estruturas de dados e estabelece conexão com servidores Barrel
     * via remota. Tenta conectar a todos os barrels configurados.
     * @param numThreads 
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    public Downloader(int numThreads) throws RemoteException {
        this.numThreads = numThreads;
        //inicia sets no construtor
        this.urlsProcessadas = ConcurrentHashMap.newKeySet();
        this.palavrasProcessadas = ConcurrentHashMap.newKeySet();
        this.contagemPalavras = new ConcurrentHashMap<>();
        this.stopWords = new HashSet<>();
        this.barrels = new ArrayList<>();
        this.barrelAtualIndex = new AtomicInteger(0);

        carregarStopWords();
        conectarAosBarrels();


        if (this.barrels.isEmpty()) {
            System.err.println("Downloader não conseguiu se conectar com nenhum Barrel.");
            System.exit(1); // Sair
        }


        this.executorService = Executors.newFixedThreadPool(numThreads);    //define o número de threads
    }

    //Tenta conectar a todos os barrels listados no config.properties 
    private void conectarAosBarrels() {
        try {
            //carrega propriedades usando o ClassLoader
            Properties properties = new Properties();

            try (InputStream input = Downloader.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    System.out.println("Desculpe, não foi possível encontrar config.properties");
                    return;
                }
                properties.load(input);
            }
            
            // Obter todas as URLs dos barrels a partir das propriedades
            String barrelUrlsString = properties.getProperty("barrel.urls");
            if (barrelUrlsString == null || barrelUrlsString.trim().isEmpty()) {
                System.err.println("Nenhuma URL de Barrel encontrada em config.properties. Use a propriedade 'barrel.urls' (ex: rmi://host/barrel1,rmi://host/barrel2).");
                return;        //Não vai p/ frente sem URLs de barrels
            }

            String[] barrelUrls = barrelUrlsString.split(","); // Divide as URLs pela vírgula
            System.out.println("URLs de Barrel configuradas: " + Arrays.toString(barrelUrls));

            // Tenta conectar a cada barrel configurado
            for (String barrelUrl : barrelUrls) {
                String trimmedUrl = barrelUrl.trim();
               
                if (!trimmedUrl.isEmpty()) {
                    try {
                        System.out.println("Tentando conectar ao Barrel em: " + trimmedUrl);
                        InterfaceBarrel connectedBarrel = (InterfaceBarrel) Naming.lookup(trimmedUrl);
                        this.barrels.add(connectedBarrel); // Adiciona à lista de barrels conectados
                        System.out.println("Conectado ao Barrel: " + trimmedUrl);
                    }
                    catch (Exception e) {
                        System.err.println("Erro ao conectar ao Barrel em " + trimmedUrl + ": " + e.getMessage());
                        // Continua tentando conectar aos outros barrels mesmo se 1 falhar
                    }
                }
            }

            if (this.barrels.isEmpty()) {
                System.err.println("Não foi possível conectar a nenhum Barrel.");
            } 
            else {
                 System.out.println("Conectado a " + this.barrels.size() + " Barrel(s) ativo(s).");
            }

        }
        catch (IOException e) {
            System.err.println("Erro ao carregar config.properties: " + e.getMessage());
        }
    }

    //Carrega as stop words a partir de um arquivo externo, no caso o txt
    private void carregarStopWords() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("stopwords.txt"));
            stopWords.addAll(lines);
        } 
        catch (IOException e) {
            System.err.println("Erro ao carregar stop words: " + e.getMessage());
        }
    }

    /**
     * Atualiza o arquivo de stop words adicionando palavras que apareçam com muita frequência
     * Palavras que aparecem + de 100 vezes são adicionadas ao arquivo.
     */
    private void atualizarStopWords() {
        System.out.println("Tentando atualizar stop words...");
        Set<String> novasStopWords = new HashSet<>();
        
        contagemPalavras.forEach((palavra, contagem) -> {
            if (StopwordClassificador.ehProvavelStopword(palavra, contagem.get(), 1) && !stopWords.contains(palavra)) { // Verifica se já não é stopword
                novasStopWords.add(palavra);
            }
        });

        if (!novasStopWords.isEmpty()) {
            System.out.println("Novas stop words candidatas: " + novasStopWords);
            
            synchronized(fileWriteLock) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("stopwords.txt", true))) {
                    for (String palavra : novasStopWords) {
                         if (!stopWords.contains(palavra)) {
                             writer.write(palavra + "\n");
                             stopWords.add(palavra);
                             System.out.println("Adicionada nova stop word: " + palavra);
                         }
                    }
                } 
                catch (IOException e) {
                    System.err.println("Erro ao atualizar arquivo de stop words: " + e.getMessage());
                }
            }
        } 
        else {
            System.out.println("Nenhuma nova stop word frequente encontrada para adicionar.");
        }
    }

    //Salva a relação palavra -> URL no urlsIndexados.txt
    private void salvarURLNoArquivo(String palavra, String url) {
        //se não tiver no arquivo, add a nova entrada
        String novaEntrada = palavra + " -> " + url;
        
        synchronized (fileWriteLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("urlsIndexados.txt", true))) {
                 writer.write(novaEntrada + "\n");
            } 
            catch (IOException e) {
                System.err.println("Erro ao salvar URL no arquivo urlsIndexados.txt: " + e.getMessage());
            }
        }
    }


    //Inicia o processamento contínuo pegando URLs dos barrels
    void comecaProcessar() {
        if (barrels.isEmpty()) {
            System.err.println("Não foi possível iniciar o processamento: Sem conexão com o Barrel.");
            return;
       }

       System.out.println("Iniciando processamento de URLs dos Barrels com " + numThreads + " threads.");

       // Cria e submete uma tarefa para cada thread 
       for (int i = 0; i < numThreads; i++) {
           executorService.submit(() -> {
               try {
                   processarFilaDosBarrels();
               } 
               catch (InterruptedException e) {
                System.err.println("Thread interrompida.");
                Thread.currentThread().interrupt();   //Restaura status de interrupção
            } 
            catch (Exception e) {
                System.err.println("Erro em uma thread de processamento:");
                e.printStackTrace();
            }
           });
       }
    }

    /**
     * o que cada thread executa:
     * Tenta pegar URLs das filas dos Barrels conectados, processa-as, extrai palavras,
     * indexa no Barrel e encontra novos links, enviando-os de volta para as filas dos Barrels.
     * @throws InterruptedException 
     */
    private void processarFilaDosBarrels() throws InterruptedException { 
        try {
            while (!Thread.currentThread().isInterrupted()) {
                 //vê se o limite de páginas desta instância foi atingido
                if (contagemPaginas.get() >= MAX_PAGES) {
                    System.out.println("Thread " + Thread.currentThread() + ": Limite máximo de páginas atingido. Encerrando thread.");
                    break;    //encerra essa thread
                }

                String url = null;
                boolean urlFound = false;
                int barrelsTentados = 0;
                int numeroDeBarrels = barrels.size();      //pega o número de barrels conectados

                if (numeroDeBarrels == 0) {
                    System.err.println("Thread " + Thread.currentThread() + ": Nenhum Barrel disponível. Aguardando...");
                    Thread.sleep(10000);     // Espera mais tempo se nenhum barrel estiver conectado
                    continue;
                }

                 // Tenta pegar uma URL de um dos barrels disponíveis, alternando entre eles
                 while(barrelsTentados < numeroDeBarrels) {
                    InterfaceBarrel barrelAtual = null;
                   
                    try {
                        //pega o índice do próximo barrel a tentar
                        int indexPraTentar = barrelAtualIndex.getAndUpdate(i -> (i + 1) % numeroDeBarrels);
                        barrelAtual = barrels.get(indexPraTentar);

                        System.out.println("Thread do Downloader " + Thread.currentThread() + ": Tentando pegar URL do Barrel: " + barrelAtual.getNomeBarrel());

                        url = barrelAtual.get_url();

                        if (url != null) {
                            System.out.println("Thread do Downloader " + Thread.currentThread() + ": URL recebida do Barrel '" + barrelAtual.getNomeBarrel() + "': '" + url + "'");
                            urlFound = true;
                            break;    //URL encontrada, sai do loop interno
                        } else {
                            System.out.println("Thread do Downloader " + Thread.currentThread() + ": Barrel '" + barrelAtual.getNomeBarrel() + "' fila vazia.");
                            barrelsTentados++;    //Conta esse barrel como tentado
                        }
                    }
                    catch (RemoteException e) {
                        System.err.println("Thread " + Thread.currentThread() + ": Erro ao tentar pegar URL: " + e.getMessage());
                        barrelsTentados++;   //conta como tentado por causa do erro
                    }
                    catch (IndexOutOfBoundsException e) {
                         System.err.println("Thread " + Thread.currentThread() + ": Índice de barrel inválido. Número de barrels: " + numeroDeBarrels);
                         barrelsTentados++;
                    }
                }

                if (!urlFound) {
                    // Todos os barrels tentados tavam vazios ou deram erro, espera antes de tentar novamente
                    System.out.println("Thread " + Thread.currentThread() + ": Filas dos Barrels tavam vazias ou inacessíveis. Esperando...");
                    try {
                        Thread.sleep(5000);   //espera 5 segundos
                    }
                    catch (InterruptedException ie) {
                        System.err.println("Thread de espera interrompida.");
                        Thread.currentThread().interrupt();    //restaura interrupção
                        break;   //sai do loop se foi interrompido enquanto dormia
                    }
                    continue;    //volta pro início do loop p/ tentar pegar outra URL
                }


                //processamento da URL obtida 
                System.out.println("Downloader Thread " + Thread.currentThread() + ": Processando URL: " + url);

                // Verifica se já processou a URL
                if (!urlsProcessadas.add(url)) {
                    System.out.println("URL já processada por esta instância: " + url);
                    continue; // Pega a próxima URL
                }

                // Incrementa contador local e verifica limite local
                int currentPageNum = contagemPaginas.incrementAndGet();
                if (currentPageNum > MAX_PAGES) {
                    System.out.println("Thread " + Thread.currentThread() + ": Limite máximo de páginas atingido. Encerrando.");
                    urlsProcessadas.remove(url);  //remove pois não vai ser processada pela thread por causa do limite
                    contagemPaginas.decrementAndGet();  //decremente pois não vai ser processada
                    break;    //sai do loop p essa thread
                }

                try {
                    // 1. Baixar a página
                    Document doc = Jsoup.connect(url)
                                        .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                                        .timeout(10000)
                                        .ignoreHttpErrors(true)
                                        .followRedirects(true)
                                        .get();

                    if (doc.connection().response().statusCode() >= 200 && doc.connection().response().statusCode() < 300) {

                        // 2. Extrair e processar palavras
                        String textoCompleto = doc.text();
                        String[] palavras = textoCompleto.toLowerCase().split("\\s+");

                        for (String palavra : palavras) {
                            palavra = palavra.replaceAll("[^a-zA-Z0-9áéíóúâêîôûãõç]", "");
                            
                            if (palavra.length() > 3 && !stopWords.contains(palavra) && !palavra.trim().isEmpty()) {
                                contagemPalavras.computeIfAbsent(palavra, k -> new AtomicInteger(0)).incrementAndGet();
                                String chaveUnica = palavra + "_" + url;
                                
                                // Vê se o par palavra - url já foi processado por essa instância do downloader
                                if (palavrasProcessadas.add(chaveUnica)) {
                                    try {
                                        // Envia p/ um Barrel aleatório indexar 
                                        if (!barrels.isEmpty()) {
                                            int barrelEscolhidoAleatoriamente = random.nextInt(barrels.size());
                                            InterfaceBarrel barrelDestino = barrels.get(barrelEscolhidoAleatoriamente);
                                            System.out.println("Thread " + Thread.currentThread() + ": Enviando indexação de '" + palavra + "' para " + url + " para Barrel: " + barrelDestino.getNomeBarrel());
                                            
                                            barrelDestino.indexar_URL(palavra, url);
                                            salvarURLNoArquivo(palavra, url);      //salva localmente também
                                        } else {
                                             System.err.println("Thread " + Thread.currentThread() + ": Não há barrels disponíveis para indexar.");
                                        }
                                    } 
                                    catch (RemoteException e) {
                                        System.err.println("Thread " + Thread.currentThread() + ": Erro ao indexar '" + palavra + "' para " + url + ": " + e.getMessage());
                                        palavrasProcessadas.remove(chaveUnica);     //desfaz adição se falhou
                                    }
                                }
                            }
                        }

                        // 3. Encontra novos links e ENVIA DE VOLTA pro barrel
                        Elements links = doc.select("a[href]");
                        for (Element link : links) {
                            String nextUrl = link.absUrl("href").trim();
                            
                            //vê se é um link http/https válido
                            if (nextUrl.startsWith("http")) {
                                //add a URL encontrada de volta à fila de um barrel aleatório
                                try {
                                    //verifica urlsProcessadas antes de enviar p/ evitar que esta instância adicione URLs que ela mesma já processou
                                    if (!urlsProcessadas.contains(nextUrl)) {        
                                        if (!barrels.isEmpty()) {
                                            int barrelEscolhidoAleatoriamente = random.nextInt(barrels.size());
                                            InterfaceBarrel barrelDestino = barrels.get(barrelEscolhidoAleatoriamente);
                                            
                                            System.out.println("Thread " + Thread.currentThread() + ": Enviando novo link '" + nextUrl + "' para a fila do Barrel: " + barrelDestino.getNomeBarrel());
                                            barrelDestino.adicionarURLNaFila(nextUrl);
                                        } 
                                        else {
                                             System.err.println("Thread " + Thread.currentThread() + ": Não há barrels disponíveis para adicionar novo link.");
                                        } 
                                    }
                                } catch (RemoteException e) {
                                     System.err.println("Thread " + Thread.currentThread() + ": Erro RMI ao enviar novo link '" + nextUrl + "' para Barrel: " + e.getMessage());
                                }
                            }
                        }
                     } else {
                          System.out.println("Thread " + Thread.currentThread() + ": Ignorando URL com status " + doc.connection().response().statusCode() + ": " + url);
                     }
                } 
                catch (HttpStatusException e) {
                     System.err.println("Thread " + Thread.currentThread() + ": Erro HTTP " + e.getStatusCode() + " ao acessar: " + url);
                } 
                catch (Exception e) {
                    System.err.println("Thread " + Thread.currentThread() + ": Erro inesperado ao processar URL: " + url);
                    e.printStackTrace();
                }
            } 
        } 
        finally {
            System.out.println("Thread " + Thread.currentThread() + " finalizada.");
        }
    }

    /**
     * Encerra o pool de threads e tenta atualizar as stop words uma última vez
     */
    public void shutdown() {
        System.out.println("Encerrando Downloader...");
        executorService.shutdown();
       
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Pool de threads não encerrou completamente após 1 min. Forçando...");
                executorService.shutdownNow();
                
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool de threads não encerrou.");
            }
        } 
        catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Pool de threads encerrado.");

        atualizarStopWords();  //atualiza as stopwords uma última vez
        System.out.println("Downloader encerrado.");
    }

    //INCIAR O DOWNLODER
    public static void main(String[] args) {
        int numThreads = 5; //num de threads

        try {
            Downloader downloader = new Downloader(numThreads);
            downloader.comecaProcessar();  //inicia as threads que vao pedir urls pro barrel
          
            //
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Recebido sinal de encerramento. Iniciando shutdown do Downloader...");
                downloader.shutdown(); // Encerra as threads após um tempo
            }));
          
            System.out.println("Downloader iniciado. Threads aguardando URLs do Barrel...");
        }
        catch (Exception e) {
            System.err.println("Erro ao inicializar o Downloader: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}