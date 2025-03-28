import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FiltrarDuplicatas {
    public static void main(String[] args) {
        String nomeArquivo = "/home/cecilia/SD-FCTUC/PROJETO/SistemasDistr/urlsIndexados.txt"; // Altere para o nome do seu arquivo

        try {
            // Lê todas as linhas do arquivo
            List<String> linhas = Files.readAllLines(Paths.get(nomeArquivo));

            // Usa um Set para remover duplicatas
            Set<String> linhasUnicas = new LinkedHashSet<>(linhas); // Mantém a ordem original

            // Reescreve o arquivo apenas com as linhas únicas
            Files.write(Paths.get(nomeArquivo), linhasUnicas, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("Arquivo filtrado com sucesso! Duplicatas removidas.");
        } catch (IOException e) {
            System.err.println("Erro ao processar o arquivo: " + e.getMessage());
        }
    }
}