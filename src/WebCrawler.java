import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.rmi.Naming;
import java.util.HashSet;
import java.util.Set;
import java.util.Properties;
import java.io.InputStream;

/**
 * Classe responsável por realizar a extração de links de páginas da web e enviá-los para indexação.
 */
public class WebCrawler {
    private Set<String> visitedLinks = new HashSet<>();      //guarda os links visitados
    private static final int MAX_PAGES = 10;                 //limite de páginas para evitar loops infinitos

    //pra poder filtrar as STOPWORDS
    /*private Classifier classifier;
    private Instances trainingData;*/

    private InterfaceGatewayServer gateway;       //interface p/ comunicar com o GATEWAYSERVER

    //conectar ao gatewayserver
    public WebCrawler() {
        try {
            // Carregar propriedades usando o ClassLoader
            Properties properties = new Properties();
            try (InputStream input = WebCrawler.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    System.out.println("Desculpe, não foi possível encontrar config.properties");
                    return;
                }
                properties.load(input);
            }

            // Obter o IP do servidor a partir das propriedades
            String serverIp = properties.getProperty("server.ip", "localhost");
            String gatewayUrl = "rmi://" + serverIp + "/server";

            System.out.println("Tentando conectar ao Crawler em: " + gatewayUrl);
            this.gateway = (InterfaceGatewayServer) Naming.lookup(gatewayUrl);
            System.out.println("Conectado ao Crawler!");

            //Carrega os dados de treinamento do arquivo ARFF e treina o modelo
            /*DataSource source = new DataSource("stopWords.arff");
            trainingData = source.getDataSet();

            // Verifica se o ARFF tem pelo menos um atributo além da classe
            if (trainingData.numAttributes() < 2) {
                throw new IllegalArgumentException("O dataset precisa ter pelo menos um atributo de texto além da classe.");
            }

            // Define o índice do atributo de classe
            trainingData.setClassIndex(trainingData.numAttributes() - 1);

            // Aplica StringToWordVector
            trainingData = aplicarStringToWordVector(trainingData);

            // Treina o classificador NaiveBayes
            classifier = new NaiveBayes();
            classifier.buildClassifier(trainingData);*/
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
        WebCrawler crawler = new WebCrawler();             //o obj WebCrawler é criado e recebe a gatewayUrl como parâmetro p/ ter acesso ao GATEWAYSERVER pela conexão RMI
        crawler.crawl("https://oglobo.globo.com/"); // URL inicial
    }
}