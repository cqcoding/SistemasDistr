import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class WebCrawler {
    private Set<String> visitedLinks = new HashSet<>(); // Armazena links visitados
    private static final int MAX_PAGES = 10; // Limite de páginas para evitar loops infinitos

    public void crawl(String url) {
        if (visitedLinks.size() >= MAX_PAGES) return; // Para depois de atingir o limite
        if (visitedLinks.contains(url)) return; // Evita visitar a mesma página

        try {
            System.out.println("Visitando: " + url);
            visitedLinks.add(url);

            Document doc = Jsoup.connect(url).get(); // Faz requisição HTTP e baixa a página
            Elements links = doc.select("a[href]"); // Encontra todos os links na página

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
        WebCrawler crawler = new WebCrawler();
        crawler.crawl("https://example.com"); // URL inicial
    }
}