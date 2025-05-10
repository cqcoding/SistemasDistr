package com;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
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

/*Classe responsável por consumir URLs da fila de um Barrel remoto, baixar o conteúdo, extrair palavras, 
encontrar novos links (e enviá-los de volta ao Barrel) e indexar palavras em um servidor Barrel via RMI, usando múltiplas threads
*/
public class Downloader{
    InterfaceBarrel barrel;     //conexão RMI com barrel
    private final Set<String> urlsProcessadas;    //URLs já processadas/visitadas pelo downloader
    private final Set<String> palavrasProcessadas;   //pares palavra_url já enviados
    private final ConcurrentMap<String, AtomicInteger> contagemPalavras;
    private final Set<String> stopWords;
    private final int numThreads;  //número de threads a serem usadas
    
    private final ExecutorService executorService; // Pool de threads para processamento

    /** Limite de páginas para evitar loops infinitos (aplicado a este Downloader). */
    private static final int MAX_PAGES = 10;     //limite por instância do Downloader
    private final AtomicInteger contagemPaginas = new AtomicInteger(0);     //contador de páginas processadas 

    private final Object fileWriteLock = new Object();     //lock para sincronizar escrita no arquivo
    /**
     * Construtor da classe Downloader.
     * Inicializa as estruturas de dados e estabelece conexão com um servidor Barrel
     * aleatório via remota.
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

        carregarStopWords();
        conectarAoBarrel();

        this.executorService = Executors.newFixedThreadPool(numThreads);    //define o número de threads
    }

    //Tenta conectar a um barrel aleatório 
    private void conectarAoBarrel() {
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

            // Obter o IP do servidor a partir das propriedades
            String serverIp = properties.getProperty("server.ip", "localhost");
            String[] barrelUrls = {
                    "rmi://" + serverIp + "/barrel1",
                    "rmi://" + serverIp + "/barrel2",
                    "rmi://" + serverIp + "/barrel3"
            };

            // Escolhe um barrel aleatório para se conectar
            Random rand = new Random();
            String barrelUrl = barrelUrls[rand.nextInt(barrelUrls.length)];

            System.out.println("Tentando conectar ao Barrel em: " + barrelUrl);
            this.barrel = (InterfaceBarrel) Naming.lookup(barrelUrl);
            System.out.println("Conectado ao Barrel!");
        } 
        catch (Exception e) {
            System.err.println("Erro ao conectar ao Barrel.");
            e.printStackTrace();
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
            if (contagem.get() > 100 && !stopWords.contains(palavra)) { // Verifica se já não é stopword
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


    //Inicia o processamento contínuo pegando URLs do barrel remoto
    void comecaProcessar() {
        if (barrel == null) {
            System.err.println("Não foi possível iniciar o processamento: Sem conexão com o Barrel.");
            return;
       }

       System.out.println("Iniciando processamento de URLs do Barrel com " + numThreads + " threads.");

       // Cria e submete uma tarefa para cada thread 
       for (int i = 0; i < numThreads; i++) {
           executorService.submit(() -> {
               try {
                   processarFilaDoBarrel();
               } 
               catch (InterruptedException e) {
                   e.printStackTrace();
               }
           });
       }
    }

    /**
     * o que cada thread executa:
     * Pega URLs da fila do Barrel, processa-as, extrai palavras, indexa no Barrel e encontra novos links,
     * enviando-os de volta para a fila do Barrel.
     * @throws InterruptedException 
     */
    private void processarFilaDoBarrel() throws InterruptedException { 
        try {
            while (!Thread.currentThread().isInterrupted()) {
                 //vê se o limite de páginas desta instância foi atingido
                if (contagemPaginas.get() >= MAX_PAGES) {
                    System.out.println("Thread " + Thread.currentThread() + ": Limite máximo de páginas atingido. Encerrando thread.");
                    break;    //encerra essa thread
                }

                String url = null;
                try {
                    //pega URL da fila do barrel
                    url = barrel.get_url(); 
                    // ---- LOG ------
                    System.out.println("Downloader Thread " + Thread.currentThread() + ": URL recebida de get_url(): '" + url + "'");
                    
                    if (url == null) {
                        // Fila do Barrel vazia, espera um pouco antes de tentar novamente
                         System.out.println("Thread " + Thread.currentThread() + ": Fila do Barrel vazia. Aguardando...");
                         try {
                             Thread.sleep(5000); // Espera 5 segundos
                         } 
                         catch (InterruptedException ie) {
                             Thread.currentThread().interrupt();    //restaura interrupção
                             break;  //sai do loop se interrompido enquanto dormia
                         }
                        continue; //vVolta ao início do loop p/ tentar pegar outra URL
                    }
                } 
                catch (RemoteException e) {
                    System.err.println("Thread " + Thread.currentThread() + ": Erro ao tentar pegar URL do Barrel: " + e.getMessage());
                    Thread.sleep(10000);  //espera
                    continue;                    //tenta novamente no próximo ciclo do loop
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
                    urlsProcessadas.remove(url);
                    contagemPaginas.decrementAndGet();
                    break;
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
                                
                                if (palavrasProcessadas.add(chaveUnica)) {
                                    try {
                                        barrel.indexar_URL(palavra, url);  //envia para o Barrel indexar
                                        salvarURLNoArquivo(palavra, url);
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
                                //add a URL encontrada de volta à fila do Barrel 
                                try {
                                    //verifica urlsProcessadas antes de enviar
                                    if (!urlsProcessadas.contains(nextUrl)) {        
                                        barrel.adicionarURLNaFila(nextUrl); 
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

        atualizarStopWords();
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