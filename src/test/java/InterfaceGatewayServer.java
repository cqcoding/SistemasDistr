import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Interface que define os métodos para o Gateway Server, permitindo a comunicação remota para indexação de URLs, pesquisa, navegação de páginas de resultados
 * e envio de URLs para processamento em um Barrel.
 * Todos os métodos desta interface lançam a exceção RemoteException, pois eles são definidos para operação remota, exigindo comunicação via RMI.
 */
public interface InterfaceGatewayServer extends Remote {
    /**
     * Indexa uma nova URL para ser processada e armazenada.
     * @param url URL a ser indexada.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    void indexar_URL(String url) throws RemoteException;     
    
    /**
     * Realiza uma pesquisa por uma palavra-chave e retorna uma lista com os resultados.
     * @param palavra palavra-chave a ser pesquisada.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    List<String> pesquisar(String palavra) throws RemoteException; 

    /**
     * Envia a URL para queue no Barrel.
     * 
     * @param url URL a ser enviada para processamento.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    void enviarURLParaProcessamento(String url) throws RemoteException; 

    /**
     * Navega para a próxima página de resultados da pesquisa.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    String next_page() throws RemoteException;                  

    /**
     * Retorna à página anterior de resultados da pesquisa.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    String previous_page() throws RemoteException;              

    /**
     * Retorna a lista de links associados à página atual.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    List<String> links_to_page() throws RemoteException;    

    /**
     * Retorna as estatísticas relacionadas à pesquisa realizada.
     * @throws RemoteException -> caso ocorrer um erro de comunicação remota.
     */
    String pagina_estatisticas() throws RemoteException;  
}
