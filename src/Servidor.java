import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;
import java.io.InputStream;

/**
 * Classe responsável por iniciar o servidor RMI e registrar o objeto que oferece o serviço, garantindo que fique disponível para os clientes.
 * O servidor cria um objeto remoto chamado `GatewayServer` e o associa a um nome de registro.
 */
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
            String objName = "rmi://" + serverIp + "/server";

            GatewayServer server = new GatewayServer(serverIp);   // Instancia o servidor

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