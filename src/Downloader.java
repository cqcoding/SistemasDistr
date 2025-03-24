import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;


public class Downloader {
    InterfaceBarrel barrel;  //conexão com o barrel

    public Downloader() throws RemoteException {
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
                if (url == null){ 
                    System.out.println("Nenhuma URL disponível");
                    break;                                 // se aquela url for nula, n tiver nenhum link, daí sai do loop
                }

                System.out.println("Baixando: " + url);
                Document doc = Jsoup.connect(url).get();   // carrega a url que está no jsoup
                Elements anchors = doc.select("a");

                // envia novas URLs encontradas para indexar
                for (Element anchor : anchors) {
                    String href = anchor.attr("href");
                    if (!href.isEmpty()) {                 // se a url encontrada não estiver vazia daí adiciona pra indexar
                    barrel.put_url(href);
                }
            }

                // processa palavras e envia ao gateway
                String[] palavras = Jsoup.parse(doc.html()).wholeText().split(" ");
                for (String palavra : palavras) {
                    palavra = palavra.trim();
                    if (palavra.length() > 3) {                // filtra as palavras curtas
                        barrel.indexar_URL(palavra, url);     // envia palavra e url p/ gateway

                        //salva a palavra e a URL no arquivo (sobre a url que o cliente quer indexar)
                        salvarURLNoArquivo(palavra, url);
                    }
                }

                System.out.println("Página processada e enviada ao Barrel.");
                Thread.sleep(1000);                    //p evita sobrecarga do servidor
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