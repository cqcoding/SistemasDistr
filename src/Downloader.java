import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Classe responsável por baixar páginas da web, processar palavras e indexar URLs em um servidor Barrel via remota.
 */
public class Downloader {
    InterfaceBarrel barrel;  //conexão com o barrel
    private Set<String> urlsProcessadas;
    private Set<String> palavrasProcessadas;
    private Map<String, Integer> contagemPalavras;
    private Set<String> stopWords;

/**
     * Construtor da classe Downloader.
     * Inicializa as estruturas de dados e estabelece conexão com um servidor Barrel aleatório via remota.
     *
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    public Downloader() throws RemoteException {
        // inicializar os Sets no construtor.
        this.urlsProcessadas = new HashSet<>();
        this.palavrasProcessadas = new HashSet<>();
        this.contagemPalavras = new HashMap<>();
        this.stopWords = new HashSet<>();
        carregarStopWords();

        try {
            String[] barrelUrls = {
            "rmi://192.168.1.164/barrel1",
            "rmi://192.168.1.164/barrel2",
            "rmi://192.168.1.164/barrel3"
            };

            // escolhe um Barrel aleatório para se conectar.
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

    /**
     * Carrega as stop words a partir de um arquivo externo, no caso o txt.
     */
    private void carregarStopWords() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("stopwords.txt"));
            stopWords.addAll(lines);
        } catch (IOException e) {
            System.err.println("Erro ao carregar stop words: " + e.getMessage());
        }
    }

    /**
     * Atualiza o arquivo de stop words adicionando palavras que apareçam com alta frequência.
     * Palavras que aparecem mais de 100 vezes são adicionadas ao arquivo.
     */
    private void atualizarStopWords() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("stopwords.txt", true))) {
            // realiza a filtragem das palavras.
            contagemPalavras.entrySet().stream()
                .filter(entry -> entry.getValue() > 100)
                .forEach(entry -> {
                    try {
                        writer.write(entry.getKey() + "\n");
                        palavrasProcessadas.add(entry.getKey());
                    } catch (IOException e) {
                        System.err.println("Erro ao atualizar stop words: " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("Erro ao abrir arquivo de stop words: " + e.getMessage());
        }
    }

    /**
     * Salva o que o cliente inserir de URL para indexar.
     * Entra na fila e o Downloader coleta da fila.
     * @param palavra Palavra associada à URL.
     * @param url URL a ser salva.
     */
    private void salvarURLNoArquivo(String palavra, String url) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("urlsIndexados.txt", true))) {
            writer.write(palavra + " -> " + url + "\n");  // salva a palavra e a URL no arquivo.
            System.out.println("URL salva: " + palavra + " -> " + url);
        } 
        catch (IOException e) {
            System.err.println("Erro ao salvar URL no arquivo: " + e.getMessage());
        }
    }

   /**
     * Executa o processo de download, extração e indexação de páginas da web.
     * As URLs são obtidas do Barrel, processadas e novas URLs são enviadas para indexação.
     */
    public void executar(){
        try {
            while (true) {
                String url = barrel.get_url();             // obtém a próxima URL para baixar.

                if (url == null || urlsProcessadas.contains(url)) {                 // verifica se a URL já foi processada.
                    System.out.println("URL nula ou já processada: " + url);
                    continue;
                }

                System.out.println("Baixando: " + url);
                urlsProcessadas.add(url);  // marca a URL como processada.
                Document doc = Jsoup.connect(url).get();   // carrega a URL que está no Jsoup.
                Elements anchors = doc.select("a");

                // envia novas URLs encontradas para indexar.
                for (Element anchor : anchors) {
                    String href = anchor.attr("href");
                   // só adiciona URLs que ainda não foram processadas.
                   if (!href.isEmpty() && !urlsProcessadas.contains(href)) {
                    barrel.put_url(href);
                }
            }

                // processa palavras e envia ao Gateway.
                String[] palavras = Jsoup.parse(doc.html()).wholeText().split(" ");
                for (String palavra : palavras) {
                    palavra = palavra.trim().toLowerCase();  // remove espaços e converte para minúsculas.

                     // verifica se a palavra é uma stop word.
                     if (stopWords.contains(palavra)) {
                        continue; // pula a palavra se for uma stop word.
                    }

                    contagemPalavras.put(palavra, contagemPalavras.getOrDefault(palavra, 0) + 1);

                     // cria uma chave única para a palavra + URL.
                     String chaveUnica = palavra + "_" + url;
                     if (palavra.length() > 3 && !palavrasProcessadas.contains(chaveUnica)) {
                         barrel.indexar_URL(palavra, url);
                         palavrasProcessadas.add(chaveUnica);
                         salvarURLNoArquivo(palavra, url);
                     }
                }

                atualizarStopWords();

                System.out.println("Página processada e enviada ao Barrel.");
                Thread.sleep(1000);                    // para evitar sobrecarga do servidor.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Método principal para iniciar o Downloader.
     */
    public static void main(String[] args) {
        Downloader down;
        try {
            down = new Downloader();
            down.executar();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}