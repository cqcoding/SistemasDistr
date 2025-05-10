import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

/**
 * Classe responsável por registrar os servidores Barrel no RMI.
 * Cria e registra três instâncias de BarrelServer, tornando-as disponíveis para comunicação remota.
 * O registro é feito utilizando o protocolo RMI e a classe Naming.
 * A classe garante que, caso haja algum erro durante o processo de registro, seja tratado e uma mensagem de erro seja exibida.
 */
public class RegistrarBarrels {
    public static void main(String[] args) {
        try {
            /** Carregar propriedades usando o ClassLoader. */
            Properties properties = new Properties();
            try (InputStream input = RegistrarBarrels.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    System.out.println("Desculpe, não foi possível encontrar config.properties");
                    return;
                }
                properties.load(input);
            }

            /** Obter o IP do servidor a partir das propriedades. */
            String serverIp = properties.getProperty("server.ip", "localhost");
            LocateRegistry.createRegistry(1099);

            /** Criar e registrar os BarrelServers. */
            for (int i = 1; i <= 3; i++) {
                String nomeBarrel = "Barrel" + i;
                BarrelServer barrel = new BarrelServer(nomeBarrel);
                String barrelName = "rmi://" + serverIp + "/barrel" + i;
                Naming.rebind(barrelName, barrel);
                System.out.println("Barrel registrado: " + barrelName);
            }

            System.out.println("Todos os barrels registrados.");
        } catch (Exception e) {
            System.err.println("Erro ao registrar os barrels: " + e.getMessage());
            e.printStackTrace();
        }
    }
}