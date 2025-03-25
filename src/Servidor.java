import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class Servidor {
    public static void main(String[] args) {
        try{
            GatewayServer server = new GatewayServer();   //instancia o servidor  -- objeto que quer fornecer via rede
            String objName = "rmi://192.168.1.164/server";    //associa esse objeto ao nome gatewayserver  -- cria nome para o objeto

            System.out.println("Registrando objeto no RMIRegistry...");

            try{
                LocateRegistry.createRegistry(1099);  //cria um registry RMI na porta 1099 (porta padrão do rmi) - se não existir, se não ele avisa, por isso o tratamento de exceção
                System.out.println("Registry RMI criado na porta 1099.");
            } 
            catch(Exception e){
                System.out.println("Registry RMI já existente.");
            }


            Naming.rebind(objName, server);         //registra o objeto remoto no RMI Registry
                
            System.out.println("Servidor RMI pronto...");
        } 
        catch (Exception e) {
            e.printStackTrace();  //Caso dê erro, ele será impresso

        }
    }
}


/*classe responsável por iniciar o servidor RMI, 
registrar o serviço e garantir que ele fique disponível para os clientes.*/