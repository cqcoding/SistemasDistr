import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Interface para o Barrel
public interface InterfaceBarrel extends Remote {
    void indexar_URL(String palavra, String url) throws RemoteException;  //indexar novo url
    List<String> pesquisar(String palavra) throws RemoteException;        //pesquisar algo e retornar lista com resultados
}