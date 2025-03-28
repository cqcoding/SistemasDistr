import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Interface que define os métodos para o Barrel.
 * Esta interface também inclui métodos para gerenciamento de filas de URLs e operações de confiabilidade.
 */
public interface InterfaceBarrel extends Remote {
    /**
     * Indexa uma nova URL associada a uma palavra-chave.
     * @param palavra palavra-chave que será associada à URL.
     * @param url URL que será indexada.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    void indexar_URL(String palavra, String url) throws RemoteException;

    /**
     * Realiza uma pesquisa por uma palavra-chave e retorna uma lista de resultados.
     * @param palavra palavra-chave a ser pesquisada.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    List<String> pesquisar(String palavra) throws RemoteException; 

    /**
     * Confirma o recebimento de uma URL associada a uma palavra-chave e implementa Reliable Multicast com ACK.
     * 
     * @param palavra palavra-chave associada à URL.
     * @param url URL que foi recebida.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    boolean confirmarRecebimento(String palavra, String url) throws RemoteException;

    /** Verifica se o Barrel está ativo. */
    boolean estaAtivo() throws RemoteException;

    /** Sincroniza os dados. */
    void sincronizarDados(Map<String, List<String>> dados) throws RemoteException;

    /** Obtém todos os dados indexados. */
    Map<String, List<String>> getIndexados() throws RemoteException;
    void adicionarURLNaFila(String url) throws RemoteException;
    String obterProximaURL() throws RemoteException;
    int tamanhoFilaURLs() throws RemoteException;       //quantas URLs ainda estão aguardando indexação
    
    String getNomeBarrel() throws RemoteException;      //método para identificar os barrels pq se não no cliente aparece o nome do proxy RMI em vez do nome do barrel
    int getTamanhoIndice() throws RemoteException;      //pras estatisticas - mostrar tamanho indice

    String get_url() throws RemoteException;
    void put_url(String url) throws RemoteException;
    boolean isQueueEmpty() throws RemoteException;
}