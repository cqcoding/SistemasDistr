import java.rmi.registry.*; // permite conexão com um servidor remoto via RMI (interface de programação que permite a execução de chamadas remotas no estilo RPC).
import java.util.*;
import org.jsoup.*; // permite baixar e processar HTML de páginas web.
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class WebCrawler {
    public static void main(String[] args) {
        try {
            // conexão com o servidor de indexação:
            Index index = (Index) LocateRegistry.getRegistry(8183).lookup("index"); 
            // LocateRegistry.getRegistry(8183): se conecta ao servidor RMI na porta 8183.
            // lookup("index"): busca um objeto remoto chamado "index", usado para armazenar URLs e textos indexados.
            Set<String> visitedUrls = new HashSet<>(); // guarda URLs já processadas (evita visitas repetidas).
            Queue<String> urlQueue = new LinkedList<>(); // fila de URLs a serem exploradas. 
            
            // loop para processar URLs:
            while (true) {
                String url = index.takeNext(); // obtém a próxima URL do servidor.
                if (url == null || visitedUrls.contains(url)) {
                    continue;
                // se a URL for nula ou já tiver sido visitada, vai p próxima.
                }
                
                System.out.println("Crawling: " + url);
                try {
                    // baixando e processando a página:
                    Document doc = Jsoup.connect(url).get(); // faz o download do HTML da página.
                    
                    // extração de texto da página:
                    String text = doc.body().text(); // extrai todo o texto da página.
                    System.out.println("Texto extraído:\n" + text.substring(0, Math.min(text.length(), 200)) + "...\n");
                    // substring(0, Math.min(text.length(), 200)): mostra apenas os primeiros 200 caracteres no console.

                    // envia a URL e o texto extraído para o servidor de indexação via RMI:
                    index.addToIndex(url, text);
                    
                    // extrai links e adiciona à fila:
                    Elements links = doc.select("a[href]"); // seleciona todos os links <a href="..."> da página.
                    for (Element link : links) {
                        String absUrl = link.absUrl("href"); // obtém o URL absoluto do link.
                    // URL absoluto do link: endereço completo da página da web, incluindo o protocolo (http:// ou https://), domínio e caminho.
                        if (!visitedUrls.contains(absUrl)) {
                            urlQueue.add(absUrl);
                        // se o link ainda não foi visitado, adiciona à fila urlQueue.
                        }
                    }
                    
                    visitedUrls.add(url); // adiciona a URL atual ao conjunto de URLs visitadas.
                    
                    // adicionar novas URLs ao índice para processamento futuro:
                    while (!urlQueue.isEmpty()) {
                        index.addToQueue(urlQueue.poll());
                    // enquanto houver URLs na fila, elas são enviadas para o servidor RMI, garantindo que sejam processadas futuramente.
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar: " + url);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        // se houver erro, exibe uma mensagem, mas o crawler não para.
        }
    }
}
