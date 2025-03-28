import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * Classe responsável por iniciar o servidor RMI e registrar o objeto que oferece o serviço, garantindo que fique disponível para os clientes.
 * O servidor cria um objeto remoto chamado `GatewayServer` e o associa a um nome de registro.
 */
public class Servidor {
    public static void main(String[] args) {
        try{
            /** Instancia o servidor. */
            GatewayServer server = new GatewayServer();  

            /** Objeto que quer fornecer via rede e o associa ao nome gatewayserver. */
            String objName = "rmi://192.168.1.164/server";

            System.out.println("Registrando objeto no RMIRegistry...");

            try{
                /** Se não existir, cria um registry RMI na porta 1099 (porta padrão do RMI) - se não, ele avisa, por isso o tratamento de exceção. */
                LocateRegistry.createRegistry(1099);  
                System.out.println("Registry RMI criado na porta 1099.");
            } 
            catch(Exception e){
                System.out.println("Registry RMI já existente.");
            }

            /** Registra o objeto remoto no RMI Registry. */
            Naming.rebind(objName, server); 
                
            System.out.println("Servidor RMI pronto...");
        } 
        catch (Exception e) {
            e.printStackTrace();  // caso der erro, será impresso.

        }
    }
}