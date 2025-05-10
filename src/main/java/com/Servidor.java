package com;

import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

/**
 * Classe responsável por iniciar o servidor RMI e registrar o objeto que oferece o serviço, garantindo que fique disponível para os clientes.
 * O servidor cria um objeto remoto chamado `GatewayServer` e o associa a um nome de registro.
 */
public class Servidor {
    public static void main(String[] args) {
        try {
            /** Carregar propriedades usando o ClassLoader. */
            Properties properties = new Properties();
            try (InputStream input = Servidor.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    System.out.println("Desculpe, não foi possível encontrar config.properties");
                    return;
                }
                properties.load(input);
            }

            /** Obter o IP do servidor a partir das propriedades. */
            String serverIp = properties.getProperty("server.ip", "localhost");
            
            /** Objeto que quer fornecer via rede. */
            String objName = "rmi://" + serverIp + "/server";

            GatewayServer server = new GatewayServer(serverIp);
            System.out.println("Registrando objeto no RMIRegistry...");

            try {
                /** Se não existir, cria um registry RMI na porta 1099 (porta padrão do RMI) - se não, ele avisa, por isso o tratamento de exceção. */
                LocateRegistry.createRegistry(1099);  
                System.out.println("Registry RMI criado na porta 1099.");
            } catch (Exception e) {
                System.out.println("Registry RMI já existente.");
            }

            /** Registra o objeto remoto no RMI Registry. */
            Naming.rebind(objName, server);

            System.out.println("Servidor RMI pronto...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}