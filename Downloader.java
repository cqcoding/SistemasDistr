import java.rmi.Naming;
import java.rmi.RemoteException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;


public class Downloader {
    InterfaceBarrel barrel;  //conexão com o barrel

    public Downloader() throws RemoteException {
        try {
            //conecta BarrelServer
            String barrelUrl = "rmi://192.168.1.164/barrel";  
            this.barrel = (InterfaceBarrel) Naming.lookup(barrelUrl);
            System.out.println("Conectado ao Barrel!");
        } 
        catch (Exception e) {
            System.err.println("Erro ao conectar ao Barrel.");
            e.printStackTrace();
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