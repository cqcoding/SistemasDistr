package org.jsoup;
import java.rmi.registry.*;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class WebCrawler {
    public static void main(String[] args) {
        try {
            Index index = (Index) LocateRegistry.getRegistry(8183).lookup("index");
            Set<String> visitedUrls = new HashSet<>();
            Queue<String> urlQueue = new LinkedList<>();
            
            while (true) {
                String url = index.takeNext();
                if (url == null || visitedUrls.contains(url)) {
                    continue;
                }
                
                System.out.println("Crawling: " + url);
                try {
                    Document doc = Jsoup.connect(url).get();
                    
                    // Extração de texto da página
                    String text = doc.body().text();
                    System.out.println("Texto extraído:\n" + text.substring(0, Math.min(text.length(), 200)) + "...\n");
                    
                    // Enviar dados para o servidor de indexação
                    index.addToIndex(url, text);
                    
                    // Extração de links e adição à fila
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String absUrl = link.absUrl("href");
                        if (!visitedUrls.contains(absUrl)) {
                            urlQueue.add(absUrl);
                        }
                    }
                    
                    visitedUrls.add(url);
                    
                    // Adicionar novas URLs ao índice para processamento futuro
                    while (!urlQueue.isEmpty()) {
                        index.addToQueue(urlQueue.poll());
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar: " + url);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
