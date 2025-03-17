import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class GatewayServer extends UnicastRemoteObject implements InterfaceGatewayServer{
    //precisa do -extends UnicastRemoteObject- pois ele faz automaticamente a exportação dos objetos remotos para que os clientes 
    //consigam chamá-lo remotamente

    private List<InterfaceBarrel> barrels;

    private static final String[] palavras_chave = {""}; // Definir palavras chave aqui
    

    protected GatewayServer() throws RemoteException {    //protegido para garantir que só classes filhas ou dentro do mesmo
        // pacote possam instanciar o objeto diretamente
        super();              //exporta o objeto remoto automaticamente, sem isso, o objeto não ficaria disponível para chamadas remotas
        this.barrels = new ArrayList<>();         //inicializa a lista de URLs
        conectarBarrels();                                 //carrega os URLs existentes no arquivo (se tiver)
    }

    private void conectarBarrels() {
        try {
            // Procurar os BarrelServers no RMI Registry
            for (int i = 1; i <= 3; i++) { // Supondo 3 barrels
                String barrelName = "rmi://localhost/barrel" + i;
                InterfaceBarrel barrel = (InterfaceBarrel) Naming.lookup(barrelName);
                barrels.add(barrel);
                System.out.println("Conectado ao barrel: " + barrelName);
            }
        } catch (Exception e) {
            System.err.println("Erro ao conectar aos barrels: " + e.getMessage());
            e.printStackTrace();
        }
    }


    //   INDEXAR URL     = salvar
    @Override
    public void indexar_URL(String url) throws RemoteException {
        // Distribuir a indexação para os barrels
        for (InterfaceBarrel barrel : barrels) {
            for (String palavra : palavras_chave){
                barrel.indexar_URL(palavra, url); // Indexar a URL em cada barrel
            }

        }
        System.out.println("URL indexada em todos os barrels: " + url);
    }


    //   PESQUISAR
    @Override
    public List<String> pesquisar(String palavra) throws RemoteException {
        List<String> resultados = new ArrayList<>();
        // Consultar cada barrel e combinar os resultados
        for (InterfaceBarrel barrel : barrels) {
            List<String> barrelResultados = barrel.pesquisar(palavra);
            resultados.addAll(barrelResultados);
        }
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