//import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Implementação do Barrel Server
class BarrelServer extends UnicastRemoteObject implements InterfaceBarrel {
    private Map<String, List<String>> urlsIndexados;

    protected BarrelServer() throws RemoteException {
        super();
        urlsIndexados = new HashMap<>();        //estrutura de dados que guarda chave(palavra pesquisada) e valor(url)
    }

    @Override
    public void indexar_URL(String palavra, String url) throws RemoteException {
        urlsIndexados.computeIfAbsent(palavra, k -> new ArrayList<>()).add(url);
        System.out.println("URL indexado: " + palavra + " -> " + url);
    }

    @Override
    public List<String> pesquisar(String palavra) throws RemoteException {
        return urlsIndexados.getOrDefault(palavra, new ArrayList<>());
    }
}
