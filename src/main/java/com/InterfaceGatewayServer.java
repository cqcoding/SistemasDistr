package com;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

//todos os metodos tem que lançar essa exceção "remoteexception" pra não dar erros, como é remoto né
public interface InterfaceGatewayServer extends Remote {
    void indexar_URL(String url) throws RemoteException;          //indexar novo url
    List<String> pesquisar(String palavra) throws RemoteException;    //pesquisar algo e retornar lista com resultados
    void enviarURLParaProcessamento(String url) throws RemoteException; // envia url pra queue no barrel
    String next_page() throws RemoteException;                  //ir p/ próxima página de resultados
    String previous_page() throws RemoteException;              //voltar p/ página anterior
    List<String> links_to_page() throws RemoteException;        //retornar links associados à página atual
    String pagina_estatisticas() throws RemoteException;    
    List<String> obterPesquisasMaisFrequentes() throws RemoteException; //obter pesquisas mais frequentes
    Map<String, Integer> obterBarrelsAtivos() throws RemoteException; //obter barrels ativos
    Map<String, Double> obterTemposResposta() throws RemoteException; //obter tempos de resposta
    String gerarAnaliseContextualizada(String query, List<String> citacoes) throws RemoteException;
    List<String> consultarRelacoes(String url) throws RemoteException;
}