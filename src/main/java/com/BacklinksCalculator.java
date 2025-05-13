package com;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BacklinksCalculator {
    
    // O método calcularBacklinks recebe um mapa de links de saída (origem -> destinos) e 
    // retorna um novo mapa onde a chave é uma URL de destino e o valor é uma lista de URLs 
    // de origem que apontam para ela (backlinks).

    /**
     * Calcula os backlinks (links que apontam para uma página específica).
     * @param linksSaida Mapa de links de saída.
     * @return Um mapa onde a chave é a URL de destino e o valor é uma lista de URLs que apontam para ela.
     */
    public Map<String, List<String>> calcularBacklinks(Map<String, List<String>> linksSaida) {
        Map<String, List<String>> backlinks = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : linksSaida.entrySet()) {
            String origem = entry.getKey();
            List<String> destinos = entry.getValue();

            for (String destino : destinos) {
                backlinks.computeIfAbsent(destino, k -> new ArrayList<>()).add(origem);
            }
        }

        return backlinks;
    }
}