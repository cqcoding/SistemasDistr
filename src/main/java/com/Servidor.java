package com;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Servidor {
    public static void main(String[] args) {
        try {
            // Carregar propriedades usando o ClassLoader
            Properties properties = new Properties();
            try (InputStream input = Servidor.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    System.out.println("Desculpe, não foi possível encontrar config.properties");
                    return;
                }
                properties.load(input);
            }

            // Obter o IP do servidor a partir das propriedades
            
            String serverIp = properties.getProperty("server.ip", "localhost");
            
            /** Obter as URLs dos barrels a partir das propriedades. */
            String barrelUrlsString = properties.getProperty("barrel.urls");
            List<String> barrelUrls = new ArrayList<>(); // Importar java.util.ArrayList
            
            if (barrelUrlsString != null && !barrelUrlsString.trim().isEmpty()) {
                String[] urlsArray = barrelUrlsString.split(",");
               
                for (String url : urlsArray) {
                    barrelUrls.add(url.trim()); // Adiciona a URL após remover espaços em branco
                }
            } 
            else {
                System.err.println("Propriedade 'barrel.urls' não encontrada ou vazia em config.properties.");
            }
            
            
            
            String objName = "rmi://" + serverIp + "/server";

            GatewayServer server = new GatewayServer(serverIp, barrelUrls);   // Instancia o servidor com os argumentos necessários

            System.out.println("Registrando objeto no RMIRegistry...");

            try {
                LocateRegistry.createRegistry(1099);  // Cria um registry RMI na porta 1099
                System.out.println("Registry RMI criado na porta 1099.");
            } catch (Exception e) {
                System.out.println("Registry RMI já existente.");
            }

            Naming.rebind(objName, server);  // Registra o objeto remoto no RMI Registry

            System.out.println("Servidor RMI pronto...");
        } catch (Exception e) {
            e.printStackTrace();  // Caso dê erro, ele será impresso
        }
    }
}


/*classe responsável por iniciar o servidor RMI, 
registrar o serviço e garantir que ele fique disponível para os clientes.*/