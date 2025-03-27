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

public class Downloader {
    InterfaceBarrel barrel;  //conexão com o barrel
    private Set<String> urlsProcessadas;
    private Set<String> palavrasProcessadas;
    private Map<String, Integer> contagemPalavras;
    private Set<String> stopWords;



    public Downloader() throws RemoteException {
        // Inicializar os Sets no construtor
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

            //escolhe um barrel aleatório p se conectar
            Random rand = new Random();
            String barrelUrl = barrelUrls[rand.nextInt(barrelUrls.length)]; // Escolhe um aleatório

            System.out.println("Tentando conectar ao Barrel em: " + barrelUrl);
            this.barrel = (InterfaceBarrel) Naming.lookup(barrelUrl);
            System.out.println("Conectado ao Barrel!");
        } 
        catch (Exception e) {
            System.err.println("Erro ao conectar ao Barrel.");
            e.printStackTrace();
        }
    }

    private void carregarStopWords() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("stopwords.txt"));
            stopWords.addAll(lines);
        } catch (IOException e) {
            System.err.println("Erro ao carregar stop words: " + e.getMessage());
        }
    }


    private void atualizarStopWords() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("stopwords.txt", true))) {
            // Filtra palavras que aparecem mais de um certo número de vezes, por exemplo, 100
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

    //isso é pra salvar o que o cliente colocar de url pra INDEXAR, entra na fila e o downloader vai pegar da fila 
    private void salvarURLNoArquivo(String palavra, String url) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("urlsIndexados.txt", true))) {
            writer.write(palavra + " -> " + url + "\n");  // Salva a palavra e a URL no arquivo
            System.out.println("URL salva: " + palavra + " -> " + url);
        } 
        catch (IOException e) {
            System.err.println("Erro ao salvar URL no arquivo: " + e.getMessage());
        }
    }

   

    public void executar(){
        try {
            while (true) {
                String url = barrel.get_url();             // pega a próxima URL para baixar

                if (url == null || urlsProcessadas.contains(url)) {                 // Verifica se a URL já foi processada
                    System.out.println("URL nula ou já processada: " + url);
                    continue;
                }

                System.out.println("Baixando: " + url);
                urlsProcessadas.add(url);  //marca URL como processada
                Document doc = Jsoup.connect(url).get();   // carrega a url que está no jsoup
                Elements anchors = doc.select("a");

                // envia novas URLs encontradas para indexar
                for (Element anchor : anchors) {
                    String href = anchor.attr("href");
                   // Só adiciona URLs que ainda não foram processadas
                   if (!href.isEmpty() && !urlsProcessadas.contains(href)) {
                    barrel.put_url(href);
                }
            }

                // processa palavras e envia ao gateway
                String[] palavras = Jsoup.parse(doc.html()).wholeText().split(" ");
                for (String palavra : palavras) {
                    palavra = palavra.trim().toLowerCase();  //remove espaços e converte para minúsculas

                     // Verifica se a palavra é uma stop word
                     if (stopWords.contains(palavra)) {
                        continue; // Pula a palavra se for uma stop word
                    }

                    contagemPalavras.put(palavra, contagemPalavras.getOrDefault(palavra, 0) + 1);

                     // Cria uma chave única para palavra+URL
                     String chaveUnica = palavra + "_" + url;
                     if (palavra.length() > 3 && !palavrasProcessadas.contains(chaveUnica)) {
                         barrel.indexar_URL(palavra, url);
                         palavrasProcessadas.add(chaveUnica);
                         salvarURLNoArquivo(palavra, url);
                     }
                }

                atualizarStopWords();

                System.out.println("Página processada e enviada ao Barrel.");
                Thread.sleep(1000);                    //p evitar sobrecarga do servidor
            }
        } catch (Exception e) {
            e.printStackTrace();
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