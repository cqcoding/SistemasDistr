import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class GatewayServer extends UnicastRemoteObject implements InterfaceGatewayServer{
    //precisa do -extends UnicastRemoteObject- pois ele faz automaticamente a exportação dos objetos remotos para que os clientes consigam chamá-lo remotamente
    
    private static final String ArquivoURLS = "urlsIndexados.txt";    //final faz com que a variável ArquivoURLS não possa ser alterada depois de inicializada. Ou seja, o valor "urlsIndexados.txt" será fixo
    //lista p/ armazenar os URLs na memória enquanto o servidor estiver ativo:
    private List<String> urlsIndexados;

    protected GatewayServer() throws RemoteException {    //protegido para garantir que só classes filhas ou dentro do mesmo pacote possam instanciar o objeto diretamente
        super();              //exporta o objeto remoto automaticamente, sem isso, o objeto não ficaria disponível para chamadas remotas
        this.urlsIndexados = new ArrayList<>();         //inicializa a lista de URLs
        carregarURLs();                                 //carrega os URLs existentes no arquivo (se tiver)
    }


    //   INDEXAR URL     = salvar
    @Override     
    public void indexar_URL(String url) throws RemoteException {
        //vê se o URL já não ta salvo
        if (!urlsIndexados.contains(url)) {
            urlsIndexados.add(url);             //add o URL à lista na memória
            salvarURL(url);                     //chama o método para salvar o URL no arquivo
            System.out.println("URL indexado: " + url);
        } 
        else {
            System.out.println("URL já foi indexado.");
        }      
    }


    //   PESQUISAR
    @Override          //método p/ pesquisar URLs que contêm a palavra-chave
    public List<String> pesquisar(String palavra) throws RemoteException {
        List<String> resultados = new ArrayList<>();      //lista que armazena os resultados da pesquisa

        //ver cada URL na lista dos salvos
        for (String url : urlsIndexados) {         //p/ cada elemento url na lista urlsIndexados, executa o código do loop - percorre todas as URLs armazenadas em urlsIndexados uma por uma
            //se o URL tem a palavra-chave, adiciona na lista de resultados
            if (url.contains(palavra)) {
                resultados.add(url);
            }
        }
        //retorna os resultados da pesquisa
        return resultados;
    }

    //   SALVAR URL
    //salvar um URL no arquivo de texto
    private void salvarURL(String url) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ArquivoURLS, true))) {
            //add o URL ao final do arquivo, com uma nova linha
            writer.write(url);
            writer.newLine();
        } 
        catch (IOException e) {
            e.printStackTrace();  //mostra o erro se der ruim ao salvar
        }
    }


    //   CARREGAR   URLS AO INICIAR SERV
    //carregar os URLs já indexados ao iniciar o servidor
    private void carregarURLs() {
        File Arquivo = new File(ArquivoURLS);

        if (Arquivo.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(ArquivoURLS))) {
                String line;
                
                //lê o arquivo linha por linha  
                while ((line = reader.readLine()) != null) {
                    urlsIndexados.add(line);     //adiciona à lista de URLs indexados
                }
            } 
            catch (IOException e) {
                e.printStackTrace(); 
            }
        }
    }

    @Override
    public String next_page() throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'next_page'");
    }

    @Override
    public String previous_page() throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'previous_page'");
    }

    @Override
    public List<String> links_to_page() throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'links_to_page'");
    }

    @Override
    public String pagina_estatisticas() throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'pagina_estatisticas'");
    }
     
}