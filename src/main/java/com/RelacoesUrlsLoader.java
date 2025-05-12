package com;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelacoesUrlsLoader {

    /**
     * Carrega as relações entre URLs a partir de um arquivo.
     * @param filePath Caminho para o arquivo de URLs indexados.
     * @return Um mapa onde a chave é a URL e o valor é uma lista de palavras-chave ou URLs que apontam para ela.
     */
    public Map<String, List<String>> carregarRelacoes(String filePath) {
        Map<String, List<String>> urlRelations = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (linha.contains("->")) {
                    String[] partes = linha.split("->");
                    String palavraChave = partes[0].trim();
                    String url = partes[1].trim();

                    // Adiciona a URL como chave e a palavra-chave como referência
                    urlRelations.computeIfAbsent(url, k -> new ArrayList<>()).add(palavraChave);
                }
            }
            System.out.println("Relações entre URLs carregadas com sucesso.");
        } catch (IOException e) {
            System.err.println("Erro ao carregar relações entre URLs: " + e.getMessage());
        }

        return urlRelations;
    }
}