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
    boolean estaAtivo() throws RemoteException; //ver se o Barrel está ativo
    void sincronizarDados(Map<String, List<String>> dados) throws RemoteException; //sincronizar de dados
    Map<String, List<String>> getIndexados() throws RemoteException; //obter todos os dados indexados
    void adicionarURLNaFila(String url) throws RemoteException;
    String obterProximaURL() throws RemoteException;
    int tamanhoFilaURLs() throws RemoteException;       //quantas URLs ainda estão aguardando indexação
    
    String getNomeBarrel() throws RemoteException;      //método para identificar os barrels pq se não no cliente aparece o nome do proxy RMI em vez do nome do barrel
    int getTamanhoIndice() throws RemoteException;      //pras estatisticas - mostrar tamanho indice

    String get_url() throws RemoteException;
    void put_url(String url) throws RemoteException;
    boolean isQueueEmpty() throws RemoteException;
}