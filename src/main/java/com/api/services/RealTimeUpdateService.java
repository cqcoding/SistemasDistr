package com.api.services; // Use o seu pacote de serviços

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.Map; // Para enviar dados estruturados, como estatísticas

@Service
public class RealTimeUpdateService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RealTimeUpdateService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Envia uma atualização de dados de estatísticas para o tópico "/topic/statistics".
     * @param statisticsData Um mapa contendo os dados das estatísticas.
     */
    public void sendStatisticsUpdate(Map<String, Object> statisticsData) {
        System.out.println("Enviando atualização de estatísticas via WebSocket: " + statisticsData);
        messagingTemplate.convertAndSend("/topic/statistics", statisticsData);
    }

    /**
     * Envia uma notificação genérica para o tópico "/topic/notifications".
     * @param message A mensagem de notificação.
     */
    public void sendNotification(String message) {
        Map<String, String> notificationPayload = Map.of("message", message);
        System.out.println("Enviando notificação via WebSocket: " + notificationPayload);
        messagingTemplate.convertAndSend("/topic/notifications", notificationPayload);
    }

    // Pode adicionar mais métodos para diferentes tipos de atualizações em tempo real
    // Por exemplo, para enviar resultados de pesquisa atualizados:
    // public void sendSearchResultsUpdate(String searchTerm, List<com.api.SearchResult> results) {
    //     Map<String, Object> payload = Map.of("searchTerm", searchTerm, "results", results);
    //     messagingTemplate.convertAndSend("/topic/searchResults/" + searchTerm.replaceAll("\\s+", "_"), payload);
    // }
}