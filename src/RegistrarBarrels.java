import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * Classe responsável por registrar os servidores Barrel no RMI.
 * Cria e registra três instâncias de BarrelServer, tornando-as disponíveis para comunicação remota.
 * O registro é feito utilizando o protocolo RMI e a classe Naming.
 * A classe garante que, caso haja algum erro durante o processo de registro, seja tratado e uma mensagem de erro seja exibida.
 */
public class RegistrarBarrels {
    public static void main(String[] args) {
        try {
            /** Criar o RMI Registry. */
            LocateRegistry.createRegistry(1099);

            /** Criar e registrar os BarrelServers. */
            for (int i = 1; i <= 3; i++) {
                String nomeBarrel = "Barrel" + i; // define o nome correto.
                BarrelServer barrel = new BarrelServer(nomeBarrel);
                String barrelName = "rmi://192.168.1.164/barrel" + i;
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