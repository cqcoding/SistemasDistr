import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import com.InterfaceGatewayServer;


/**
 * Classe de teste para a comunicação com um servidor Gateway via RMI.
 * Esta classe testa funcionalidades expostas pela interface remota da Gateway.
 */
public class TesteGatewayServer {
    public static void main(String[] args) throws MalformedURLException {
        try {
            String gatewayUrl = "rmi://localhost/server";
            InterfaceGatewayServer gateway = (InterfaceGatewayServer) Naming.lookup(gatewayUrl);
            
            /** Para testar métodos RMI. */
            String urlParaIndexar = "https://example.com";

            gateway.indexar_URL(urlParaIndexar);
            System.out.println("URL indexada: " + urlParaIndexar);

            /** Palavra-chave usada para pesquisar. */
            String palavraParaPesquisar = "Bruna";

            /** Realiza uma pesquisa no Gateway. */
            List<String> resultados = gateway.pesquisar(palavraParaPesquisar);
            System.out.println("Resultados da pesquisa para '" + palavraParaPesquisar + "': " + resultados);
            
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Erro ao conectar ao GatewayServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}