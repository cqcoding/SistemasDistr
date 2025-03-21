import java.rmi.Remote;

// esse código define a comunicação remota entre o downloader e o index 
// especifica os métodos remotos que o index deve implementar para gerenciar URLs e as palavras indexadas

public interface DownIndex extends Remote {
    public String get_url() throws java.rmi.RemoteException; // retorna a próxima URL a ser baixada pelo downloader
    public void put_url(String url) throws java.rmi.RemoteException; // add uma nova URL à fila do index, permitindo que o downloader envie novos links encontrados
    public void save_words(String word, String url) throws java.rmi.RemoteException; // guarda palavras extraídas da página e associa cada uma ao seu URL
}