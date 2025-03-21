import java.rmi.Naming;
import java.rmi.RemoteException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;


public class Downloader {
    InterfaceGatewayServer gateway;  //conexão com o GatewayServer

    public Downloader() throws RemoteException {

    }

    public void executar(){
        try {
            //
            String server = "rmi://192.168.1.164/server"; 
            InterfaceGatewayServer gateway = (InterfaceGatewayServer) Naming.lookup(server); //conecta a gateway p indexar palavras e urls
           
            System.out.println("Conectado no Servidor");
            
            while (true) {
                String url = gateway.get_url();             // pega a próxima URL para baixar
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
                    gateway.put_url(href);
                }
            }

                // processa palavras e envia ao gateway
                String[] palavras = Jsoup.parse(doc.html()).wholeText().split(" ");
                for (String palavra : palavras) {
                    palavra = palavra.trim();
                    if (palavra.length() > 3) {                // filtra as palavras curtas
                        gateway.indexar_URL(palavra, url);     // envia palavra e url p/ gateway
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