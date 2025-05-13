package com.api.config; // Use o seu pacote de configuração

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Habilita o processamento de mensagens WebSocket, suportado por um message broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Define um prefixo para os destinos das mensagens que vêm do servidor para o cliente (broker)
        // Os clientes subscreverão a tópicos que começam com "/topic"
        config.enableSimpleBroker("/topic");
        // Define um prefixo para os destinos das mensagens que vêm do cliente para o servidor
        // Mensagens enviadas pelos clientes para endpoints como "/app/someEndpoint" serão roteadas
        // para métodos anotados com @MessageMapping no servidor.
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registra um endpoint WebSocket que os clientes usarão para se conectar ao servidor.
        // "/ws" é o caminho do endpoint.
        // withSockJS() habilita o fallback para SockJS se WebSockets não estiverem disponíveis no browser.
        registry.addEndpoint("/ws").withSockJS();
    }
}