import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.rmi.Naming;
import java.util.HashSet;
import java.util.Set;

public class WebCrawler {
    private Set<String> visitedLinks = new HashSet<>();      //guarda os links visitados
    private static final int MAX_PAGES = 10;                 //limite de páginas para evitar loops infinitos

    private InterfaceGatewayServer gateway;       //interface p/ comunicar com o GATEWAYSERVER

    //conectar ao gatewayserver
    public WebCrawler(String gatewayUrl) {
        try {
            this.gateway = (InterfaceGatewayServer) Naming.lookup(gatewayUrl);    //conecta ao GATEWAYSERVER via RMI
            //this.gateway recebe a interface remota - deixa chamar métodos no servidor 
            //Naming.lookup(gatewayUrl) faz uma busca no registro RMI pelo serviço disponível no endereço lá embaixo citado
        } 
        catch (Exception e) {
            System.err.println("Erro ao conectar ao GatewayServer.");
            e.printStackTrace();
        }
    }

    public void crawl(String url) {
        if (visitedLinks.size() >= MAX_PAGES) return;     //p/ depois de atingir o limite
        if (visitedLinks.contains(url)) return;           //não deixa visitar a mesma página

        try {
            System.out.println("Visitando: " + url);
            visitedLinks.add(url);

            Document doc = Jsoup.connect(url).get();           //faz requisição HTTP e baixa a página
            Elements links = doc.select("a[href]");   //encontra todos os links na página

            //enviar URL p/ o GATEWAYSERVER p/ indexar
            gateway.enviarURLParaProcessamento(url);

            for (Element link : links) {
                String nextUrl = link.absUrl("href");
                if (!visitedLinks.contains(nextUrl) && nextUrl.startsWith("http")) {
                    crawl(nextUrl); // Recursivamente visita os links encontrados
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao acessar: " + url);
        }
    }

    public static void main(String[] args) {
        String gatewayUrl = "rmi://192.168.1.164/server";    //gatewayUrl guarda a URL do servidor RMI
        WebCrawler crawler = new WebCrawler(gatewayUrl);     //o obj WebCrawler é criado e recebe a gatewayUrl como parâmetro p/ ter acesso ao GATEWAYSERVER pela conexão RMI
        crawler.crawl("https://pt.wikipedia.org/wiki/Elefante"); // URL inicial
    }
}