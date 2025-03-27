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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GatewayServer extends UnicastRemoteObject implements InterfaceGatewayServer {
    //precisa do -extends UnicastRemoteObject- pois ele faz automaticamente a exportação dos objetos remotos para que os clientes
    //consigam chamá-lo remotamente
    private List<InterfaceBarrel> barrels;
    private static final String[] palavras_chave = {""}; // Definir palavras chave aqui
    private static final String ArquivoURLS = "urlsIndexados.txt";
    private List<String> urlsIndexados;

    //estruturas necessárias p/ armazenar as estatísticas
    private final Map<String, Integer> pesquisasFrequentes;  //contador das pesquisas + comuns
    private final Map<String, Integer> barrelsAtivos;        //lista de Barrels ativos e tamanhos de índice
    private final Map<String, List<Long>> temposResposta;    //tempo médio de resposta por Barrel

    //estruturas necessárias p nextpage, previous e link
    private List<String> resultadosPesquisa;                 //resultados da última pesquisa feita - list pois não precisa estar associada a uma chave, são só urls
    private int paginaAtual;                                 //indice da pág atual
    private static final int TAMANHO_PAGINA = 10;            //nº de resultados por pág

    protected GatewayServer() throws RemoteException {    //protegido para garantir que só classes filhas ou dentro do mesmo
        // pacote possam instanciar o objeto diretamente
        super();              //exporta o objeto remoto automaticamente, sem isso, o objeto não ficaria disponível para chamadas remotas
        this.barrels = new ArrayList<>();
        this.urlsIndexados = new ArrayList<>();         //inicializa a lista de URLs
        this.pesquisasFrequentes = new HashMap<>();     //inicializa os maps das estatísticas
        this.barrelsAtivos = new HashMap<>();
        this.temposResposta = new HashMap<>();
        this.resultadosPesquisa = new ArrayList<>();
        this.paginaAtual = 0;                           //pags começam com 0

        conectarBarrels();
        carregarURLs();                                 //carrega os URLs existentes no arquivo (se tiver)
        iniciarMonitoramento();

    }

    private List<String> barrelUrls = Arrays.asList( //lista de barrels disponíveis
        "rmi://194.210.38.168/barrel1", //ip do pc, deve ser alterado dependendo do teste
        "rmi://194.210.38.168/barrel2", 
        "rmi://194.210.38.168/barrel3"
    );


    private void conectarBarrels() {

        barrels.clear();

        for(String barrelUrl: barrelUrls){
            try { 
                    // Conecta barrels usando a URL fornceida
                    InterfaceBarrel barrel = (InterfaceBarrel) Naming.lookup(barrelUrl);
                    barrels.add(barrel);  // ADiciona o barrel conectado à lista de barrels
                    System.out.println("Conectado ao barrel: " + barrelUrl);
                
            } catch (Exception e) {
                System.err.println("Erro ao conectar aos barrels " + barrelUrl);
                e.printStackTrace();
            }
        }
    }

    private void iniciarMonitoramento() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Verifica a cada 5 segundos
                    //System.out.println("Monitorando Barrels...");

                    List<InterfaceBarrel> barrelsAtivos = new ArrayList<>();
                    for (InterfaceBarrel barrel : barrels) {
                        try {
                            if (barrel.estaAtivo()) {  //verifica se cada barrel está ativo
                                barrelsAtivos.add(barrel);  // adiciona barrels ativos à lista
                            }
                        } catch (RemoteException e) {
                            System.err.println("Barrel inativo detectado!");  //quando o barrel está inativo manda aviso de erro
                        }
                    }

                    barrels = barrelsAtivos;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public boolean reliableMulticast(String palavra, String url) { //garante que os dados estao replicadso entre os barrels
        List<InterfaceBarrel> confirmados = new ArrayList<>();

        try {
            // Enviar para todos os barrels
            for (InterfaceBarrel barrel : barrels) {
                barrel.indexar_URL(palavra, url);
            }

            // Confirmar recebimento
            for (InterfaceBarrel barrel : barrels) {
                if (barrel.confirmarRecebimento(palavra, url)) {
                    confirmados.add(barrel);
                } else {
                    System.err.println("Erro: Barrel não confirmou recebimento."); //se qualquer barrel nao confirmar, a operação falha
                    return false;
                }
            }

            System.out.println("Indexação concluída com sucesso!"); //quando todos confirmam, é sucesso
            return true;
        } catch (RemoteException e) {
            System.err.println("Falha no Reliable Multicast: " + e.getMessage());
            return false;
        }
    }

    // INDEXAR URL = salvar
    @Override
    public void indexar_URL(String url) throws RemoteException {
        if (!urlsIndexados.contains(url)) {
            // Distribuir a indexação para os barrels
            for (InterfaceBarrel barrel : barrels) {
                for (String palavra : palavras_chave) {
                    barrel.indexar_URL(palavra, url); // Indexar a URL em cada barrel
                }
            }
            urlsIndexados.add(url);
            salvarURL(url);
            System.out.println("URL indexada em todos os barrels: " + url);
        } else {
            System.out.println("URL já foi indexado.");
        }
    }

    // PESQUISAR
    @Override
    public List<String> pesquisar(String palavra) throws RemoteException {
        long inicio = System.nanoTime();
        List<String> resultados = new ArrayList<>();

        // Consultar cada barrel e combinar os resultados
        for (InterfaceBarrel barrel : barrels) {
            List<String> barrelResultados = barrel.pesquisar(palavra);
            resultados.addAll(barrelResultados);
        }

        long duracao = (System.nanoTime() - inicio) / 100000;

        //atualiza a contagem da palavra pesquisada
        pesquisasFrequentes.put(palavra, pesquisasFrequentes.getOrDefault(palavra, 0) + 1);

        //simula atualização de tempo de resposta por Barrel (substituir pela lógica real)
        String barrel = "Barrel1";  // Aqui você pode associar um Barrel real da busca
        temposResposta.putIfAbsent(barrel, new ArrayList<>());
        temposResposta.get(barrel).add(duracao);

        this.resultadosPesquisa = resultados;
        this.paginaAtual = 0;

        return resultados;
    }

    // SALVAR URL
    //salvar um URL no arquivo de texto
    private void salvarURL(String url) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ArquivoURLS, true))) {
            //add o URL ao final do arquivo, com uma nova linha
            writer.write(url);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace(); //mostra o erro se der ruim ao salvar
        }
    }

    // CARREGAR URLS AO INICIAR SERV
    //carregar os URLs já indexados ao iniciar o servidor
    private void carregarURLs() {
        File arquivo = new File(ArquivoURLS);
        if (arquivo.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(ArquivoURLS))) {
                String line;
                //lê o arquivo linha por linha
                while ((line = reader.readLine()) != null) {
                    urlsIndexados.add(line); //adiciona à lista de URLs indexados
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String next_page() throws RemoteException {
        if (resultadosPesquisa.isEmpty()) {
            return "Nenhum resultado disponível.";
        }
        if ((paginaAtual + 1) * TAMANHO_PAGINA < resultadosPesquisa.size()) {
            paginaAtual++;
        }
        return String.join("\n", getResultadosPaginaAtual());
    }

    @Override
    public String previous_page() throws RemoteException {
        if (paginaAtual > 0) {
            paginaAtual--;
        }
        return String.join("\n", getResultadosPaginaAtual());
    }

    @Override
    public List<String> links_to_page() throws RemoteException {
        return getResultadosPaginaAtual();
    }

    private List<String> getResultadosPaginaAtual() {
        int inicio = paginaAtual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, resultadosPesquisa.size());
        return resultadosPesquisa.subList(inicio, fim);
    }

    @Override
    public String pagina_estatisticas() throws RemoteException {
        StringBuilder relatorioEstatisticas = new StringBuilder();  //cira objeto stringbuilder

        //TOP 10 PESQUISAS + comuns
        relatorioEstatisticas.append("Top 10 pesquisas mais comuns:\n");     //append -> add texto na stringbuilder, nesse caso um título pro nosso relatório
 
 
        pesquisasFrequentes.entrySet().stream()  // Converte o mapa pesquisasFrequentes (palavra -> quantidade) em um fluxo de dados.
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))  // Ordena os resultados do maior para o menor número de pesquisas.
            .limit(10)  // Mantém apenas os 10 mais pesquisados.
            .forEach(entry -> relatorioEstatisticas.append(entry.getKey())  // Para cada entrada, adiciona a palavra pesquisada...
                                .append(": ")  // Adiciona um separador.
                                .append(entry.getValue())  // Adiciona o número de vezes que foi pesquisada.
                                .append("\n"));  // Pula para a próxima linha.

         //LISTA DE BARRELS ATIVOS e tamanhos do índice
        relatorioEstatisticas.append("\nBarrels ativos e tamanho do índice:\n"); //titulo


        barrelsAtivos.forEach((barrel, tamanho) ->  // Percorre o mapa barrelsAtivos (Barrel -> tamanho do índice).
        relatorioEstatisticas.append(barrel)  // Adiciona o nome do Barrel.
                .append(": ")  // Adiciona um separador.
                .append(tamanho)  // Adiciona o tamanho do índice (número de URLs indexados).
                .append(" URLs\n"));  // Especifica que o valor representa URLs e pula para a próxima linha.

        relatorioEstatisticas.append("\nTempo médio de resposta por Barrel (décimos de segundo):\n"); //titulo
        for (var entry : temposResposta.entrySet()) { // Percorre o mapa temposResposta (Barrel -> lista de tempos de resposta).
            String barrel = entry.getKey(); // Obtém o nome do Barrel.
            List<Long> tempos = entry.getValue(); // Obtém a lista de tempos de resposta para esse Barrel.
            
            long media = tempos.stream()  // Converte a lista de tempos de resposta em um fluxo de dados.
                            .mapToLong(Long::longValue)  // Converte a lista de objetos Long para primitivos long.
                            .sum() / tempos.size();  // Calcula a média somando todos os valores e dividindo pelo total.

            
            relatorioEstatisticas.append(barrel)  // Adiciona o nome do Barrel.
                .append(": ")  // Adiciona um separador.
                .append(media)  // Adiciona o tempo médio de resposta.
                .append("\n");  // Pula para a próxima linha.
        }

        return relatorioEstatisticas.toString();  //Retorna o relatório final como string
    }

    //sempre que indexar algo no barrel ele atualiza o tamanho
    public void atualizarTamanhoBarrel(String barrel, int tamanho) {
        barrelsAtivos.put(barrel, tamanho);
    }

    @Override
    public void enviarURLParaProcessamento(String url) throws RemoteException {
        if (!urlsIndexados.contains(url)) {
            for (InterfaceBarrel barrel : barrels) {
                barrel.put_url(url);
            }
            urlsIndexados.add(url);
            salvarURL(url);
            System.out.println("URL enviada para processamento: " + url);
        } else {
            System.out.println("URL já foi enviada para processamento.");
        }
    }
}

