import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StopWords {
    public static Set<String> carregarWords(String caminhoArquivo) throws IOException {
        Set<String> stopWords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                stopWords.add(linha.trim().toLowerCase());
            }
        }
        return stopWords;
    }

    public static List<String> removerWords(String texto, Set<String> stopWords) {
        List<String> resultado = new ArrayList<>();
        for (String palavra : texto.toLowerCase().split("\\s+")) {
            if (!stopWords.contains(palavra)) {
                resultado.add(palavra);
            }
        }
        return resultado;
    }
}