import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

public class TesteGatewayServer {
    public static void main(String[] args) throws MalformedURLException {
        try {
            // URL do BarrelServer
            String gatewayUrl = "rmi://192.168.1.164/server";
            
            // Conectar ao BarrelServer via RMI
            InterfaceGatewayServer gateway = (InterfaceGatewayServer) Naming.lookup(gatewayUrl);
            
            //testar métodos RMI
            //URL que vai ser indexada
            String urlParaIndexar = "https://example.com";

            gateway.indexar_URL(urlParaIndexar);
            System.out.println("URL indexada: " + urlParaIndexar);
            
            String palavraParaPesquisar = "Bruna";
            List<String> resultados = gateway.pesquisar(palavraParaPesquisar);
            System.out.println("Resultados da pesquisa para '" + palavraParaPesquisar + "': " + resultados);
            
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Erro ao conectar ao GatewayServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}