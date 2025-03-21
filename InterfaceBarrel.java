import java.rmi.Remote;
import java.rmi.RemoteException;
//import java.rmi.server.UnicastRemoteObject;
//import java.util.ArrayList;
//import java.util.HashMap;
import java.util.List;
//import java.util.Map;
import java.util.Map;

// Interface para o Barrel
public interface InterfaceBarrel extends Remote {
    void indexar_URL(String palavra, String url) throws RemoteException;  //indexar novo url
    List<String> pesquisar(String palavra) throws RemoteException;        //pesquisar algo e retornar lista com resultados
    boolean confirmarRecebimento(String palavra, String url) throws RemoteException;  //implementar reliable multicast com ACK
    boolean estaAtivo() throws RemoteException;                           //método para verificar se o Barrel está ativo
    void sincronizarDados(Map<String, List<String>> dados) throws RemoteException;    //sincronização de dados
    Map<String, List<String>> getIndexados() throws RemoteException;      //método para obter todos os dados indexados
}