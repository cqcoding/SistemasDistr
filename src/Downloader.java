import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

// downloader obtém URLs do index e envia palavras para o barrel

public class Downloader {
    DownIndex index;
    Barrels barrel;

    public Downloader() throws RemoteException {

    }

    public void executar(){
        try {
            // conectando-se ao Index
            Registry registry = LocateRegistry.getRegistry(8183);
            index = (DownIndex) registry.lookup("index");
            // conectando-se ao Barrel para enviar os dados extraídos
            barrel = (Barrels) registry.lookup("barrel");
            
            System.out.println("Connected do Server");
            
            while (true) {
                String url = index.get_url(); // pega a próxima URL para baixar
                if (url == null) 
                System.out.println("Nenhuma URL disponível");
                   break; // se aquela url for nula, n tiver nenhum link, daí sai do loop

                //System.out.println("Baixando: " + url);
                Document doc = Jsoup.connect(url).get(); // carrega a url que está no jsoup
                Elements anchors = doc.select("a");

                // envia novas URLs encontradas para indexar
                for (Element anchor : anchors) {
                    String href = anchor.attr("href");
                    if (!href.isEmpty()) { // se a url encontrada não estiver vazia daí adiciona pra indexar
                    index.put_url(href);
                }
            }

                // processa palavras e envia ao barrel
                String[] palavras = Jsoup.parse(doc.html()).wholeText().split(" ");
                for (String palavra : palavras) {
                    palavra = palavra.trim();
                    if (palavra.length() > 3) { // filtra as palavras curtas
                        barrel.save_word(palavra, url); // envia palavra para o barrel
                    }
                }

                System.out.println("Página processada e enviada ao Barrel.");
                Thread.sleep(1000); // para evita sobrecarga do servidor
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