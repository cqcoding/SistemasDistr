import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class GravarNoArquivo {
    public static void main(String[] args) {
        try {
            // Abrir o arquivo para gravação (adiciona no final do arquivo)
            BufferedWriter writer = new BufferedWriter(new FileWriter("urlsIndexados.txt", true));
            
            // Gravar uma palavra-chave com a URL associada
            writer.write("kayanne -> https://www.dicionariodenomesproprios.com.br/kayane/");
            
            // Fechar o arquivo
            writer.close();
            
            System.out.println("URL gravada com sucesso.");
        } catch (IOException e) {
            System.err.println("Erro ao gravar no arquivo: " + e.getMessage());
        }
    }
}
