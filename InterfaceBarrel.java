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
    boolean confirmarRecebimento(String palavra, String url) throws RemoteException;  // implementar reliable multicast com ACK
    boolean estaAtivo() throws RemoteException; // Método para verificar se o Barrel está ativo
    void sincronizarDados(Map<String, List<String>> dados) throws RemoteException; // Sincronização de dados
    Map<String, List<String>> getIndexados() throws RemoteException; // Método para obter todos os dados indexados
    void adicionarURLNaFila(String url) throws RemoteException;
    String obterProximaURL() throws RemoteException;
    int tamanhoFilaURLs() throws RemoteException;
    
    public String get_url() throws java.rmi.RemoteException;    //retorna a próxima URL a ser baixada pelo downloader
    public void put_url(String url) throws java.rmi.RemoteException;  //add uma nova URL à fila do index, permitindo que o downloader envie novos links encontrados
}