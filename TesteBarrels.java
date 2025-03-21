import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public class TesteBarrels {
    public static void main(String[] args) throws MalformedURLException {
        try {
            // URL do BarrelServer
            String barrelUrl = "rmi://192.168.1.164/barrel1";
            
            // Conectar ao BarrelServer via RMI
            InterfaceBarrel barrel = (InterfaceBarrel) Naming.lookup(barrelUrl);
            
            // Testar métodos RMI
            barrel.indexar_URL("exemplo", "https://example.com");
            List<String> resultados = barrel.pesquisar("exemplo");
            boolean ativo = barrel.estaAtivo();
            
            System.out.println("Barrel ativo: " + ativo);
            System.out.println("Resultados da pesquisa: " + resultados);
            
            // Testar sincronização de dados
            Map<String, List<String>> dados = barrel.getIndexados();
            System.out.println("Dados indexados: " + dados);
            
            // Testar sincronização de dados entre barrels
            Map<String, List<String>> dadosParaSincronizar = Map.of("palavra2", List.of("https://example2.com"));
            barrel.sincronizarDados(dadosParaSincronizar);
            
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Erro ao conectar ao BarrelServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
