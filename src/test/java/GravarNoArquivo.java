import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Classe responsável por gravar uma URL associada a uma palavra-chave em um arquivo de texto.
 */
public class GravarNoArquivo {
    
    /**
     * Método principal que executa a gravação da URL no arquivo "urlsIndexados.txt".
     * O arquivo é aberto no modo de adição, garantindo que novos dados sejam acrescentados ao final do arquivo.
     */
    public static void main(String[] args) {
        try {
            /** Abrir o arquivo para gravação (adiciona no final do arquivo). */
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("urlsIndexados.txt", true))) {
                /** Gravar uma palavra-chave com a URL associada. */
                writer.write("kayanne -> https://www.dicionariodenomesproprios.com.br/kayane/");
            }
            
            System.out.println("URL gravada com sucesso.");
        } catch (IOException e) {
            System.err.println("Erro ao gravar no arquivo: " + e.getMessage());
        }
    }
}