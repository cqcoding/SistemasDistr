package com.api;

import com.GatewayServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.rmi.RemoteException;
import java.util.List;

@Configuration
public class Config {

    @Bean
    public GatewayServer gatewayServer() throws RemoteException {
        return new GatewayServer("localhost", List.of(
            "rmi://localhost/barrel1",
            "rmi://localhost/barrel2",
            "rmi://localhost/barrel3"
        ));
    }
}