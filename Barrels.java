import java.rmi.Remote;
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
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

// Implementação do Barrel Server
class BarrelServer extends UnicastRemoteObject implements InterfaceBarrel {
    private Map<String, List<String>> urlsIndexados;
    private static final String ArquivoURLS = "urlsIndexados.txt";
    private Queue<String> urlQueue;


    protected BarrelServer() throws RemoteException {
        super();
        urlsIndexados = new HashMap<>();        //estrutura de dados que guarda chave(palavra pesquisada) e valor(url)
        urlQueue = new ArrayDeque<>(); // Inicializa a fila de URLs
        carregarURLs();
    }

    @Override
    public void indexar_URL(String palavra, String url) throws RemoteException {
        urlsIndexados.computeIfAbsent(palavra, k -> new ArrayList<>()).add(url);
        salvarURLs();
        System.out.println("URL indexado: " + palavra + " -> " + url);
    }

    @Override
    public List<String> pesquisar(String palavra) throws RemoteException {
        return urlsIndexados.getOrDefault(palavra, new ArrayList<>());
    }

    @Override
    public boolean confirmarRecebimento(String palavra, String url) throws RemoteException {
        return urlsIndexados.containsKey(palavra) && urlsIndexados.get(palavra).contains(url);
    }  //verifica se a palavra e a url estao indexadas corretamente 

    @Override
    public boolean estaAtivo() throws RemoteException {
        return true; // Se o método for chamado com sucesso, o Barrel está ativo
    }

    @Override
    public void sincronizarDados(Map<String, List<String>> dados) throws RemoteException { //sincroniza dados entre os barrels
        urlsIndexados.putAll(dados); //adiciona ou substitui os dados no mapa urlIndexados
        salvarURLs();
        System.out.println("Dados sincronizados com sucesso.");
    }

    @Override
    public Map<String, List<String>> getIndexados() throws RemoteException {
        return urlsIndexados;  //retorna o mapa de palavras + urls indexados
    }

       // Métodos para a fila de URLs
    public void adicionarURLNaFila(String url) throws RemoteException {
        urlQueue.add(url);
        System.out.println("URL adicionada à fila: " + url);
    }

    public String obterProximaURL() throws RemoteException {
        return urlQueue.poll();
    }

    public boolean filaVazia() throws RemoteException {
        return urlQueue.isEmpty();
    }

    @Override
    public int tamanhoFilaURLs() throws RemoteException {
        return urlQueue.size();
    }
       // SALVAR URL
    //salvar um URL no arquivo de texto
    private void salvarURLs() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ArquivoURLS))) {
            for (Map.Entry<String, List<String>> entry : urlsIndexados.entrySet()) {
                String palavra = entry.getKey();
                for (String url : entry.getValue()) {
                    writer.write(palavra + " -> " + url);
                    writer.newLine();
                }
            }
            System.out.println("URLs salvas com sucesso no arquivo.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void carregarURLs() {
        File arquivo = new File(ArquivoURLS);
        if (!arquivo.exists()) return; // Se o arquivo não existir, não há nada para carregar

        try (BufferedReader reader = new BufferedReader(new FileReader(ArquivoURLS))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] partes = linha.split(" -> "); //separa a linha em duas partes palavra e url
                if (partes.length == 2) {  //se tiver separada, é porque a palavra está indexada
                    String palavra = partes[0];
                    String url = partes[1];
                    urlsIndexados.computeIfAbsent(palavra, k -> new ArrayList<>()).add(url); //se nao tiver separada ele computa uma nova url
                }
            }
            System.out.println("URLs carregadas do arquivo.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
