import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LerDoArquivo {
    public static void main(String[] args) {
        try {
            // Abrir o arquivo para leitura
            BufferedReader reader = new BufferedReader(new FileReader("urlsIndexados.txt"));
            String line;
            
            // Ler cada linha do arquivo e imprimir
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            // Fechar o arquivo
            reader.close();
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
    }
}
