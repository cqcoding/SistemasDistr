import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Downloader {
    public static void downloadPage(String url, String filePath) {
        try {
            Document doc = Jsoup.connect(url).get();
            String html = doc.html();
            FileUtils.writeStringToFile(new File(filePath), html, StandardCharsets.UTF_8);
            System.out.println("Página salva em: " + filePath);
        } catch (IOException e) {
            System.err.println("Erro ao baixar página: " + url);
        }
    }

    public static void main(String[] args) {
        String url = "https://example.com";
        String filePath = "downloaded_page.html";
        downloadPage(url, filePath);
    }
}