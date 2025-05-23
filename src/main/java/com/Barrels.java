package com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;

/** 
 * Implementação do servidor Barrel, responsável por indexar URLs associadas a palavras-chaves e gerenciar uma fila de URLs para download.
 */ 
class BarrelServer extends UnicastRemoteObject implements InterfaceBarrel {
    private final Map<String, Set<String>> urlsIndexados;
    private static final String ArquivoURLS = "urlsIndexados.txt";
    private Queue<String> urlQueue;
    private String nome; // para poder dar os nomes certinhos e para aparecer nas estatísticas.

    /**
     * Construtor do BarrelServer.
     * @param nome 
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    protected BarrelServer(String nome) throws RemoteException {
        super();
        this.nome = nome;
        urlsIndexados = new HashMap<>();        // estrutura de dados que guarda chave (palavra pesquisada) e valor(url).
        urlQueue = new ArrayDeque<>(); // inicializa a fila de URLs.
        carregarURLs();
    }

    /**
     * Retorna o nome do Barrel - para as estatísticas.
     *
     * @return Nome do Barrel
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public String getNomeBarrel() throws RemoteException {
        return nome;
    }

    /**
     * Retorna o tamanho do índice, ou seja, a quantidade total de URLs indexadas - para as estatísticas e para mostrar o que já está indexado.
     *
     * @return Número total de URLs indexadas.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public int getTamanhoIndice() throws RemoteException {
        int total = 0;
        for (Set<String> urls : urlsIndexados.values()) {  //passa por todo o urlsIndexados 
            total += urls.size();                           // \_> soma o tamanho da lista e depois retorna esse tamanho
        }
        return total;
    }

    /**
     * Indexa uma URL associada a uma palavra-chave.
     *
     * @param palavra Palavra-chave a ser indexada.
     * @param url URL associada à palavra.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public void indexar_URL(String palavra, String url) throws RemoteException {
        String palavraChave = palavra.trim().toLowerCase(); // Convertido para minúsculas pra não ser case-sensitive
        String urlParaIndexar = url.trim();

        // Verificar se a palavra é uma stopword usando o método original com valores dummy
        // Usamos 0 para frequencia e 1 para numPaginas (ou 0) para evitar as regras de frequência/página
        if (StopwordClassificador.ehProvavelStopword(palavraChave, 0, 1)) {
            System.out.println("Stopword ignorada na indexação: '" + palavraChave + "' para URL: " + urlParaIndexar);
            return; // Não indexa a stopword
        }
       
        synchronized (urlsIndexados) {
            urlsIndexados.computeIfAbsent(palavraChave, k -> new HashSet<>());
            Set<String> urls = urlsIndexados.get(palavraChave);

            if (urls.add(url)) { // retorna true se a URL não estava presente
                salvarURLs();
                System.out.println("URL indexado: " + palavraChave + " -> " + urlParaIndexar);
            } 
            else {
                System.out.println("URL já estava indexada para a palavra: " + palavraChave + " -> " + urlParaIndexar);
            }
        }
    }

    /**
     * Retorna a próxima URL da fila para o Downloader.
     *
     * @return Próxima URL da fila.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public String get_url() throws RemoteException {
        System.out.println(nome + ": get_url chamada. Tamanho atual da fila: " + urlQueue.size());
        String url = urlQueue.poll();
        return url;
    }

    /**
     * Verifica se a fila de URLs está vazia.
     *
     * @return true se a fila estiver vazia, false caso contrário.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public synchronized boolean isQueueEmpty() throws RemoteException {
        return urlQueue.isEmpty();
    }

    /**
     * Pesquisa URLs associadas a uma palavra-chave.
     * A palavra-chave da pesquisa é convertida para minúsculas.
     *
     * @param palavra Palavra-chave a ser pesquisada.
     * @return Lista de URLs associadas à palavra.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public List<String> pesquisar(String palavra) throws RemoteException {
        if (palavra == null || palavra.trim().isEmpty()){
            return new ArrayList<>(); // Retorna lista vazia se a palavra for nula ou vazia
        }
        
        String palavraChave = palavra.trim().toLowerCase(); // Convertido para minúsculas
        Set<String> urls = urlsIndexados.getOrDefault(palavraChave, new HashSet<>());
        return new ArrayList<>(urls);
    }

    /**
     * Verifica se a palavra e a URL estão indexadas corretamente.
     * A palavra-chave é convertida para minúsculas.
     * 
     * @param palavra Palavra-chave.
     * @param url URL a ser verificada.
     * @return true se a URL estiver indexada, false caso contrário.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public boolean confirmarRecebimento(String palavra, String url) throws RemoteException {
        String palavraChave = palavra.trim().toLowerCase(); // Convertido para minúsculas
        String urlParaVerificar = url.trim();
        
        return urlsIndexados.containsKey(palavraChave) && urlsIndexados.get(palavraChave).contains(urlParaVerificar);
    }

    /**
     * Verifica se o servidor Barrel está ativo.
     *
     * @return Se o método for chamado com sucesso, o Barrel está ativo, então true.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public boolean estaAtivo() throws RemoteException {
        return true;
    }

    /**
     * Sincroniza os dados entre os servidores Barrel.
     *
     * @param dados Contém as palavras-chave e suas URLs indexadas.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public void sincronizarDados(Map<String, List<String>> dados) throws RemoteException { 
        for (Map.Entry<String, List<String>> entry : dados.entrySet()) {
            String palavraChave = entry.getKey().trim().toLowerCase(); // Convertido para minúsculas
            Set<String> urlsExistentes = urlsIndexados.computeIfAbsent(palavraChave, k -> new HashSet<>());
            for (String url : entry.getValue()) {
                urlsExistentes.add(url.trim()); // como é Set, duplicadas não entram.
            }
        }
        salvarURLs();
        System.out.println("Dados sincronizados com sucesso.");
    }

    /**
     * Retorna o índice de palavras e URLs armazenadas no Barrel.
     *
     * @return Mapa contendo as palavras e as URLs indexadas.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public Map<String, List<String>> getIndexados() throws RemoteException {
        Map<String, List<String>> copia = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : urlsIndexados.entrySet()) {
            copia.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copia;
    }

    /**
     * Adiciona uma URL à fila de URLs.
     *
     * @param url URL a ser adicionada.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public void adicionarURLNaFila(String url) throws RemoteException {
        urlQueue.add(url.trim());
        System.out.println("URL adicionada à fila: " + url + ". Novo tamanho da fila: " + urlQueue.size());
    }

    /**
     * Obtém a próxima URL da fila de URLs.
     *
     * @return próxima URL na fila ou null se estiver vazia.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public String obterProximaURL() throws RemoteException {
        return urlQueue.poll();
    }


    /**
     * Retorna o tamanho da fila de URLs.
     *
     * @return Número de URLs na fila.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public int tamanhoFilaURLs() throws RemoteException {
        return urlQueue.size();
    }

    /**
     * Retorna uma lista de palavras-chave que estão associadas à URL informada.
     * Útil para descobrir quais palavras apontam para a URL (backlinks).
     *
     * @param url URL que está sendo consultada.
     * @return Lista de palavras que têm essa URL associada.
     * @throws RemoteException -> caso ocorrer um erro de comunicação RMI.
     */
    @Override
    public List<String> obterPaginasApontandoPara(String url) throws RemoteException {
        List<String> palavrasApontando = new ArrayList<>();

        String urlNormalizada = url.trim();

        for (Map.Entry<String, Set<String>> entry : urlsIndexados.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(urlNormalizada)) {
                palavrasApontando.add(entry.getKey());
            }
        }
        return palavrasApontando;
    }

    /**
     * Salva as URLs indexadas no arquivo de texto.
     */
    private void salvarURLs() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ArquivoURLS))) {
            for (Map.Entry<String, Set<String>> entry : urlsIndexados.entrySet()) {
                String palavraChave = entry.getKey();
                for (String url : entry.getValue()) {
                    writer.write(palavraChave + " -> " + url);
                    writer.newLine();
                }
            }
            System.out.println("URLs salvas com sucesso no arquivo.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carrega as URLs indexadas do arquivo de texto.
     * As palavras-chave lidas do arquivo são convertidas para minúsculas.
     */
    private void carregarURLs() {
        File arquivo = new File(ArquivoURLS);
        if (!arquivo.exists()) return; // se o arquivo não existir, não há nada para carregar.

        try (BufferedReader reader = new BufferedReader(new FileReader(ArquivoURLS))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] partes = linha.split(" -> ", 2); // separa a linha em duas partes, sendo uma de palavra e outra de URL.
                if (partes.length == 2) {  // se tiver separada é porque a palavra está indexada.
                    String palavraChave = partes[0].trim().toLowerCase(); // Convertido para minúsculas
                    String url = partes[1].trim();
                    urlsIndexados.computeIfAbsent(palavraChave, k -> new HashSet<>()).add(url); // se não estiver separada, computa uma nova URL.
                }
            }
            System.out.println("URLs carregadas do arquivo.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}