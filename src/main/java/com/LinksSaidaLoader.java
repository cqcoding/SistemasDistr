package com;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinksSaidaLoader {

    /**
     * Carrega os links de saída de cada página a partir de um arquivo.
     * @param filePath Caminho para o arquivo de links de saída.
     * @return Um mapa onde a chave é a URL de origem e o valor é uma lista de URLs de destino.
     */
    public Map<String, List<String>> carregarLinksSaida(String filePath) {
        Map<String, List<String>> linksSaida = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (linha.contains("->")) {
                    String[] partes = linha.split("->");
                    String origem = partes[0].trim();
                    String[] destinos = partes[1].trim().split(",");

                    linksSaida.put(origem, Arrays.stream(destinos)
                                                 .map(String::trim)
                                                 .toList());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar links de saída: " + e.getMessage());
        }

        return linksSaida;
    }
}