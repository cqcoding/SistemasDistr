import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;

/**
 * Classe responsável por baixar páginas da web, processar palavras e indexar URLs em um servidor Barrel via remota.
 */
public class Downloader {
    InterfaceBarrel barrel;                     //conexão com o barrel
    
    private Set<String> urlsProcessadas;        // URLs que já foram processadas
    private Set<String> palavrasProcessadas;    // p/ evitar re-indexar a mesma palavra da mesma URL
    private Set<String> stopWords;              //carregada inicialmente, atualizada dinamicamente
    
    private final ConcurrentMap<String, AtomicInteger> contagemPalavras; //contagem thread-safe
    private final AtomicLong paginasProcessadasDesdeUltimaVerificacao;   //contador thread-safe

    private static final String stopWords_file = "stopwords.txt";
    private static final int numero_threads = 5;         //nº de threads para processamento
    private final ExecutorService threadPool;
    private final Object stopWordLock = new Object(); //lock p/ operação de atualizarStopWords

/**
     * Construtor da classe Downloader.
     * Inicializa as estruturas de dados e estabelece conexão com um servidor Barrel aleatório via remota.
     *
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    public Downloader() throws RemoteException {
        // inicializar os Sets no construtor - usa implementações concorrentes
        this.urlsProcessadas = ConcurrentHashMap.newKeySet();
        this.palavrasProcessadas = ConcurrentHashMap.newKeySet();
        this.stopWords = ConcurrentHashMap.newKeySet();
        this.contagemPalavras = new ConcurrentHashMap<>();
        this.paginasProcessadasDesdeUltimaVerificacao = new AtomicLong(0);

        carregarStopWords();

        // Inicializa o pool de threads
        this.threadPool = Executors.newFixedThreadPool(numero_threads);

        //conexão rmi
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
            System.out.println("Conectado ao Barrel: " + this.barrel.getNomeBarrel());
        } 
        catch (Exception e) {
            threadPool.shutdownNow(); // Garante que o pool seja desligado se a conexão falhar

            System.err.println("Erro ao conectar ao Barrel.");
            e.printStackTrace();
        }
        //add shutdown hook para tentar desligar o pool corretamente
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * Carrega as stop words a partir de um arquivo externo, no caso o stopwords.txt
     */
    private void carregarStopWords() {
        try {
            Set<String> palavrasCarregadas;
            if (Files.exists(Paths.get(stopWords_file))) {
                palavrasCarregadas = new HashSet<>(Files.readAllLines(Paths.get(stopWords_file)));
            } else {
                System.out.println("Arquivo " + stopWords_file + " não encontrado, iniciando com lista vazia.");
                palavrasCarregadas = new HashSet<>();
            }
           
            //add ao set concorrente
            stopWords.addAll(palavrasCarregadas);
            System.out.println("Stop words carregadas: " + stopWords.size());   //mostra o temenho da lista de sotp words
        } 
        catch (IOException e) {
            System.err.println("Erro ao carregar stop words: " + e.getMessage());
        }
    }

    /**
     * Atualiza o arquivo de stop words adicionando palavras que apareçam com alta frequência.
     * Palavras que aparecem mais de 100 vezes são adicionadas ao arquivo.
     */
    private void atualizarStopWords() {
        //sincroniza a operação p/ evitar múltiplas threads fazendo isso ao mesmo tempo
        synchronized (stopWordLock) {
            Set<String> palavrasParaAdicionarArquivo = new HashSet<>();
            int threshold = 100; //limite

            //1 -> identifica localmente palavras frequentes que AINDA não estão no set em memória
            contagemPalavras.forEach((palavra, count) -> {
                if (count.get() > threshold && !stopWords.contains(palavra)) {
                    palavrasParaAdicionarArquivo.add(palavra);
                }
            });

            if (palavrasParaAdicionarArquivo.isEmpty()) {
                return; //n fazer nada
            }

            System.out.println("Tentando adicionar " + palavrasParaAdicionarArquivo.size() + " novas stop words potenciais.");

            //2 -> tenta obter lock no arquivo p/ escrita segura
            try (FileChannel channel = FileChannel.open(Paths.get(stopWords_file),
                         StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                 FileLock lock = channel.lock()) { //lock exclusivo

                System.out.println("Lock obtido em " + stopWords_file + ". Verificando e adicionando.");

                //3 -> DENTRO DO LOCK: Re-ler o arquivo p/ segurança
                 Set<String> stopWordsAtuaisNoArquivo = new HashSet<>(Files.readAllLines(Paths.get(stopWords_file)));
                 int palavrasRealmenteAdicionadas = 0;

                //4 -> add ao arquivo e ao set em memória (stopWords)
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(stopWords_file), StandardOpenOption.APPEND)) {
                    for (String palavra : palavrasParaAdicionarArquivo) {
                        if (!stopWordsAtuaisNoArquivo.contains(palavra)) {
                            writer.write(palavra);
                            writer.newLine();
                            stopWords.add(palavra);         //add ao set concorrente em memória
                            palavrasRealmenteAdicionadas++;
                        } else {
                             //se já tá no arquivo (adicionado por outra thread/instância), garante que esteja no set em memória desta instância também
                             stopWords.add(palavra);
                         }
                    }
                }

                if (palavrasRealmenteAdicionadas > 0) {
                     System.out.println(palavrasRealmenteAdicionadas + " novas stop words adicionadas ao arquivo e à memória.");
                } 
                else {
                     System.out.println("Nenhuma palavra nova precisou ser adicionada ao arquivo.");
                }

            } 
            catch (IOException e) {
                System.err.println("Erro de I/O ao tentar atualizar " + stopWords_file + ": " + e.getMessage());
            } 
            catch (Exception e) {
                System.err.println("Erro ao tentar obter lock ou atualizar " + stopWords_file + ": " + e.getMessage());
            }
       } //fim do synchronized (stopWordLock)
    }

    // Classe interna para a tarefa de processar uma URL
    private class ProcessUrlTask implements Runnable {
        private final String url;

        ProcessUrlTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                 System.out.println("[" + Thread.currentThread().getName() + "] Processando: " + url);

                 // 1. Baixar conteúdo (Jsoup)
                 Document doc = Jsoup.connect(url)
                                   .userAgent("MeuCrawlerMultiThread/1.0")
                                   .timeout(15000) // Aumentar timeout talvez
                                   .get();

                 // 2. Processar palavras
                 String textoCompleto = doc.wholeText();
                 String[] palavras = textoCompleto.split("\\s+");

                 for (String palavra : palavras) {
                     palavra = palavra.trim().toLowerCase().replaceAll("[^a-z0-9]", "");

                     // Verifica se não é stopword ANTES de indexar/contar
                     if (palavra.length() > 2 && !stopWords.contains(palavra)) {
                         // Indexar no Barrel (se ainda não indexado para esta URL)
                         String chaveIndex = palavra + "_" + url;
                         // add retorna true se o elemento não estava presente
                         if (palavrasProcessadas.add(chaveIndex)) {
                             try {
                                 barrel.indexar_URL(palavra, url);
                             } catch (RemoteException re) {
                                 System.err.println("[" + Thread.currentThread().getName() + "] Erro RMI ao indexar '" + palavra + "': " + re.getMessage());
                                 // Remover da lista de indexados se falhou? Opcional.
                                 palavrasProcessadas.remove(chaveIndex);
                             }
                         }

                         // Contar a palavra (thread-safe)
                         contagemPalavras.computeIfAbsent(palavra, k -> new AtomicInteger(0)).incrementAndGet();
                     }
                 }

                 // 3. Extrair e adicionar novos links à fila do Barrel
                 Elements links = doc.select("a[href]");
                 for (Element link : links) {
                     String nextUrl = link.absUrl("href");
                     // Verifica se é válido e se JÁ FOI SUBMETIDO para processamento por alguma thread
                     if (nextUrl != null && !nextUrl.isEmpty() && nextUrl.startsWith("http") && urlsProcessadas.add(nextUrl)) {
                         // Se add retornou true, a URL era nova neste set.
                         // Adiciona à fila do Barrel (put_url é thread-safe no servidor)
                         try {
                             barrel.put_url(nextUrl);
                         } catch (RemoteException re) {
                             System.err.println("[" + Thread.currentThread().getName() + "] Erro RMI ao adicionar link '" + nextUrl + "': " + re.getMessage());
                             // Remover de urlsProcessadas para tentar novamente depois? Opcional.
                             urlsProcessadas.remove(nextUrl);
                         }
                     }
                     // Se urlsProcessadas.add(nextUrl) retornou false, outra thread já submeteu/processou.
                 }

                 // 4. Incrementa contador de páginas processadas
                 long paginasProcessadas = paginasProcessadasDesdeUltimaVerificacao.incrementAndGet();

                 // 5. Verifica necessidade de atualizar stop words (disparado por uma das threads)
                 // Faz a verificação aqui dentro, mas a atualização em si é sincronizada
                 int checkThreshold = 50; // Verifica a cada 50 páginas (total, não por thread)
                 if (paginasProcessadas % checkThreshold == 0) {
                     System.out.println("[" + Thread.currentThread().getName() + "] Atingido threshold para verificar stop words ("+ paginasProcessadas +").");
                     atualizarStopWords(); // Método interno é sincronizado
                 }
                 System.out.println("[" + Thread.currentThread().getName() + "] Concluído: " + url);


            } catch (IOException | IllegalArgumentException e) {
                 System.err.println("[" + Thread.currentThread().getName() + "] Erro ao baixar/processar URL '" + url + "': " + e.getMessage());
                 // Não fazer nada, a URL foi marcada como processada (urlsProcessadas.add)
            } catch (Exception e) {
                  System.err.println("[" + Thread.currentThread().getName() + "] Erro inesperado processando URL '" + url + "': " + e.getMessage());
                  e.printStackTrace(); // Imprime stack trace para erros inesperados
            }
        } // Fim do run()
    } // Fim da classe interna ProcessUrlTask


    //basicamente pega as URLs do barrel, processa e encontra novos links --> alimenta o pool de threads com URLs da fila do Barrel
    public void executar(){
        if (barrel == null) {
            System.err.println("Downloader sem conexão com Barrel.");
            return;
        }
        System.out.println("Downloader iniciado com " + numero_threads + " threads.");

        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Verifica se o pool de threads está ativo
                if (threadPool.isShutdown()) {
                    System.out.println("Thread pool desligado. Encerrando loop principal.");
                    break;
                }

                String url = null;
                try {
                    url = barrel.get_url();   //obtém a próxima URL para baixar
                } 
                catch (RemoteException e) {
                     System.err.println("Erro RMI ao obter URL do Barrel: " + e.getMessage() + ". Esperando...");
                     Thread.sleep(5000);    //pausa antes de tentar de novo
                     continue;
                }
                

                if (url != null) {               //if pra verificar se a URL já foi processada
                    // Adiciona ao set ANTES de submeter para evitar que múltiplas threads peguem a mesma URL
                    // da fila rapidamente antes que a primeira possa marcá-la como processada.
                    if (urlsProcessadas.add(url)) {
                        //se add retornou true, a URL é nova, submete a tarefa
                        threadPool.submit(new ProcessUrlTask(url));
                        System.out.println("URL colocada em processamento: " + url); 
                    } 
                    else {
                        //se add retornou false, a URL já foi processada, daí ignora
                        System.out.println("URL já processada, ignorando: " + url); 
                    }
                } else {
                    //fila vazia -> espera um pouco
                    System.out.println("Fila do Barrel vazia. Esperando..."); 
                    Thread.sleep(3000);           //espera 3 segundos antes de verificar dnv
                }

                //pausa curta no loop principal p/ não sobrecarregar a CPU apenas verificando a fila
                Thread.sleep(100);

            } // Fim do while
        } catch (InterruptedException e) {
            System.out.println("Loop principal do Downloader interrompido.");
            Thread.currentThread().interrupt();
       } catch (Exception e) {
            System.err.println("Erro inesperado no loop principal do Downloader: " + e.getMessage());
            e.printStackTrace();
       } finally {
            System.out.println("Encerrando loop principal do Downloader.");
            shutdown(); // Garante que o shutdown seja chamado ao sair do loop
       }
    }

    //Desliga o pool de threads de forma organizada
    public void shutdown() {
        System.out.println("Iniciando desligamento do Downloader...");

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown(); // Desabilita novas tarefas
            
            try {
                // Espera um tempo para as tarefas existentes terminarem
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool de threads não terminou em 60 segundos, forçando desligamento...");
                    threadPool.shutdownNow(); // Cancela tarefas em execução
                    // Espera um pouco mais pelo cancelamento
                    if (!threadPool.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("Pool de threads não desligou.");
                } 
                else {
                     System.out.println("Pool de threads desligado com sucesso.");
                }
            } 
            catch (InterruptedException ie) {
                System.err.println("Interrompido durante o desligamento do pool.");
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        //tenta atualizar stop words uma última vez
        System.out.println("Tentando atualização final de stop words...");
        atualizarStopWords();
        System.out.println("Downloader desligado.");
    }

    public static void main(String[] args) {
        try {
            Downloader down = new Downloader();
            down.executar();

            //p prog pode terminar aqui se executar() retornar, mas as threads do pool continuarão rodando - o shutdown hook cuida do desligamento
        } 
        catch (RemoteException e) {
            System.err.println("Falha ao iniciar Downloader: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);    //sai se não conseguir iniciar
        }
    }
}