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
import java.util.Properties;
import java.io.InputStream;

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
            // Carregar propriedades usando o ClassLoader
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

    private void salvarURLNoArquivo(String palavra, String url) {
        try {
            // Carregar todas as URLs já salvas no arquivo
            List<String> linhas = Files.readAllLines(Paths.get("urlsIndexados.txt"));
            
            // Verificar se a URL já foi salva
            String novaEntrada = palavra + " -> " + url;
            if (linhas.contains(novaEntrada)) {
                //System.out.println("URL já salva, ignorando: " + novaEntrada);
                return; // Evita escrever duplicado
            }
    
            // Se não estiver no arquivo, adiciona a nova entrada
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("urlsIndexados.txt", true))) {
                writer.write(novaEntrada + "\n");
                System.out.println("URL salva: " + novaEntrada);
            }
        } 
        catch (IOException e) {
            System.err.println("Erro ao salvar URL no arquivo: " + e.getMessage());
        }
    }

    public void executar(){
        try {
            while (true) {
                String url = barrel.get_url();             // obtém a próxima URL para baixar.

                if (url == null) {                 // Verifica se a URL já foi processada
                    System.out.println("Nenhuma URL disponível para processasmento");
                    Thread.sleep(1000);
                    continue;
                }

                if (urlsProcessadas.contains(url)) {
                    System.out.println("URL já processada: " + url);
                    continue;
                }

                System.out.println("Baixando: " + url);
                urlsProcessadas.add(url);  // marca a URL como processada.
                Document doc = Jsoup.connect(url).get();   // carrega a URL que está no Jsoup.
                Elements anchors = doc.select("a");

                // envia novas URLs encontradas para indexar.
                for (Element anchor : anchors) {
                    String href = anchor.attr("href");
                    // Verifica se a URL é absoluta e não foi processada
                    if (!href.isEmpty() && !urlsProcessadas.contains(href) && (href.startsWith("http://") || href.startsWith("https://"))) {
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

    public void verificarFila() {
        try {
            if (barrel.isQueueEmpty()) {
                System.out.println("A fila de URLs está vazia.");
            } else {
                System.out.println("A fila de URLs contém elementos.");
            }
        } catch (RemoteException e) {
            System.err.println("Erro ao verificar a fila: " + e.getMessage());
        }
    }

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