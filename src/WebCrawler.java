/*import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;*/

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

    //pra poder filtrar as STOPWORDS
    /*private Classifier classifier;
    private Instances trainingData;*/

    private InterfaceGatewayServer gateway;       //interface p/ comunicar com o GATEWAYSERVER

    //conectar ao gatewayserver
    public WebCrawler(String gatewayUrl) {
        try {
            this.gateway = (InterfaceGatewayServer) Naming.lookup(gatewayUrl);    //conecta ao GATEWAYSERVER via RMI
            //this.gateway recebe a interface remota - deixa chamar métodos no servidor 
            //Naming.lookup(gatewayUrl) faz uma busca no registro RMI pelo serviço disponível no endereço lá embaixo citado

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

    /* 
    //MÉTODO P CONVERTER STRING EM VETORES NUMERICOS
    private Instances aplicarStringToWordVector(Instances dataset) throws Exception {
        StringToWordVector filtro = new StringToWordVector();
        filtro.setInputFormat(dataset);
        
        // Configurações do filtro
        filtro.setLowerCaseTokens(true);   // Converte tudo para minúsculas
        filtro.setWordsToKeep(1000);       // Mantém até 1000 palavras mais comuns
        filtro.setTFTransform(true);       // Usa Term Frequency (TF)
        filtro.setIDFTransform(true);      // Usa Inverse Document Frequency (IDF)
    
        // Aplica o filtro
        Instances novoDataset = Filter.useFilter(dataset, filtro);
        
        return novoDataset;
    }


    //MÉTODO P VERIFICAR SE UMA PALAVRA É STOPWORD -  usando o modelo treinado
    private boolean eStopWord(String palavra) {
        if (palavra == null || palavra.isEmpty()) {
            return false;                          //considera como não stop word se a palavra for vazia ou nula
        }

        try {
            // Criar uma instância vazia com o mesmo esquema do trainingData
            Instances instanceSet = new Instances(trainingData, 0);
            DenseInstance instance = new DenseInstance(trainingData.numAttributes());
            instance.setDataset(instanceSet);

            // Define o valor do atributo de texto transformado (ajustado pelo StringToWordVector)
            instance.setValue(trainingData.attribute(0), palavra);

            // Classifica a palavra
            double result = classifier.classifyInstance(instance);
            String classification = trainingData.classAttribute().value((int) result);

            return classification.equals("yes");
        } 
        catch (Exception e) {
            System.err.println("Erro ao classificar a palavra: " + e.getMessage());
            return false;               //se der erro - não é stop word daí
        }
    }
    */


    public void crawl(String url) {
        if (visitedLinks.size() >= MAX_PAGES) return;     //p/ depois de atingir o limite
        if (visitedLinks.contains(url)) return;           //não deixa visitar a mesma página - evita duplicação

        try {
            System.out.println("Visitando: " + url);
            visitedLinks.add(url);

            Document doc = Jsoup.connect(url).get();           //faz requisição HTTP e baixa a página
            //String text = doc.text();                          //extrai o texto - STOPWORD
            Elements links = doc.select("a[href]");            //encontra todos os links na página

            /*//filtrar STOPWORDS do texto
            String[] palavras = text.split("\\s+");           //divide em palavras
            StringBuilder filteredText = new StringBuilder();
            for (String palavra : palavras){
                palavra = palavra.toLowerCase();                    //converte para lowercase
                if (!eStopWord(palavra)){                           //checa se não é uma stopword
                    filteredText.append(palavra).append(" ");       //concatena - junta
                }
            }*/

            //enviar URL p/ o GATEWAYSERVER p/ indexar
            gateway.enviarURLParaProcessamento(url);

            for (Element link : links) {
                String nextUrl = link.absUrl("href");
                if (!visitedLinks.contains(nextUrl) && nextUrl.startsWith("http")) {
                    crawl(nextUrl);                                  //recursivamente visita os links encontrados
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao acessar: " + url);
        }
    }

    public static void main(String[] args) {
        String gatewayUrl = "rmi://192.168.1.164/server";            //gatewayUrl guarda a URL do servidor RMI
        WebCrawler crawler = new WebCrawler(gatewayUrl);             //o obj WebCrawler é criado e recebe a gatewayUrl como parâmetro p/ ter acesso ao GATEWAYSERVER pela conexão RMI
        crawler.crawl("https://oglobo.globo.com/"); // URL inicial
    }
}