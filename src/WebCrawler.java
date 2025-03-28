import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.rmi.Naming;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe responsável por realizar a extração de links de páginas da web e enviá-los para indexação.
 */
public class WebCrawler {
    /** Armazena os links visitados. */
    private Set<String> visitedLinks = new HashSet<>();
    
    /** Limite de páginas para evitar loops infinitos. */
    private static final int MAX_PAGES = 10;                
    
    /** Interface para comunicação com o GatewayServer. */
    private InterfaceGatewayServer gateway;      

    /**
     * Construtor da classe.
     * Conecta-se ao GatewayServer via RMI para envio de URLs para indexação.
     * @param gatewayUrl URL do servidor RMI para conexão.
     */
    public WebCrawler(String gatewayUrl) {
        try {
            this.gateway = (InterfaceGatewayServer) Naming.lookup(gatewayUrl);    
        } 
        catch (Exception e) {
            System.err.println("Erro ao conectar ao GatewayServer.");
            e.printStackTrace();
        }
    }

    /**
     * Realiza o processo de rastreamento (crawl) de uma página da web.
     * O rastreamento é feito de forma recursiva, respeitando o limite máximo de páginas e evitando visitas repetidas à mesma URL.
     * @param url URL da página a ser rastreada.
     */
    public void crawl(String url) {
        if (visitedLinks.size() >= MAX_PAGES) return; 
        if (visitedLinks.contains(url)) return;           // não permite visitar a mesma página - evita duplicação.

        try {
            System.out.println("Visitando: " + url);
            visitedLinks.add(url);

            /** Faz a requisição HTTp e baixa a página. */
            Document doc = Jsoup.connect(url).get();           
            
            /** Encontra todos os links na página. */
            Elements links = doc.select("a[href]");            

            /** Envia a URL para o GatewayServer para indexar. */
            gateway.enviarURLParaProcessamento(url);

            /** Recursivamente visita os links encontrados. */
            for (Element link : links) {
                String nextUrl = link.absUrl("href");
                if (!visitedLinks.contains(nextUrl) && nextUrl.startsWith("http")) {
                    crawl(nextUrl);                                  
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao acessar: " + url);
        }
    }

    public static void main(String[] args) {
        /** gatewayURL guarda a URL do servidor RMI. */
        String gatewayUrl = "rmi://192.168.1.164/server";            
        
        /** O objeto WebCrawler é criado e recebe gatewayUrl como parâmetro para ter acesso ao GatewayServer pela conexão RMI.  */
        WebCrawler crawler = new WebCrawler(gatewayUrl);   
        
        /** URL inicial. */
        crawler.crawl("https://oglobo.globo.com/");
    }
}