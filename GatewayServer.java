import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GatewayServer extends UnicastRemoteObject implements InterfaceGatewayServer{
    //precisa do -extends UnicastRemoteObject- pois ele faz automaticamente a exportação dos objetos remotos para que os clientes 
    //consigam chamá-lo remotamente
    
    //estruturas necessárias p/ armazenar as estatísticas
    private final Map<String, Integer> pesquisasFrequentes;  //contador das pesquisas + comuns
    private final Map<String, Integer> barrelsAtivos;        //lista de Barrels ativos e tamanhos de índice
    private final Map<String, List<Long>> temposResposta;    //tempo médio de resposta por Barrel

    //estruturas necessárias p nextpage, previous e link
    private List<String> resultadosPesquisa;                 //resultados da última pesquisa feita - list pois não precisa estar associada a uma chave, são só urls
    private int paginaAtual;                                 //indice da pág atual
    private static final int TAMANHO_PAGINA = 10;            //nº de resultados por pág

    private static final String ArquivoURLS = "urlsIndexados.txt";    /*final faz com que a variável ArquivoURLS não possa
    ser alterada depois de inicializada - ou seja, o valor "urlsIndexados.txt" será fixo*/
    
    //lista p/ armazenar os URLs na memória enquanto o servidor estiver ativo:
    private List<String> urlsIndexados;

    protected GatewayServer() throws RemoteException {    //protegido para garantir que só classes filhas ou dentro do mesmo
        // pacote possam instanciar o objeto diretamente
        super();              //exporta o objeto remoto automaticamente, sem isso, o objeto não ficaria disponível para chamadas remotas
        this.urlsIndexados = new ArrayList<>();         //inicializa a lista de URLs
        carregarURLs();                                 //carrega os URLs existentes no arquivo (se tiver)
       
        this.pesquisasFrequentes = new HashMap<>();     //inicializa os maps das estatísticas
        this.barrelsAtivos = new HashMap<>();
        this.temposResposta = new HashMap<>();

        this.resultadosPesquisa = new ArrayList<>();
        this.paginaAtual = 0;                           //pags começam com 0
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
        long inicio = System.nanoTime();                  //marca o tempo antes da pesquisa - chat

        List<String> resultados = new ArrayList<>();      //lista que armazena os resultados da pesquisa

        //ver cada URL na lista dos salvos
        for (String url : urlsIndexados) {         //p/ cada elemento url na lista urlsIndexados,
            // executa o código do loop - percorre todas as URLs armazenadas em urlsIndexados uma por uma
            //se o URL tem a palavra-chave, adiciona na lista de resultados
            if (url.contains(palavra)) {
                resultados.add(url);
            }
        }

        long duracao = (System.nanoTime() - inicio) / 100000;      //converte p/ décimas de segundo - chat

        //atualiza a contagem da palavra pesquisada - chat --> ver explicação certinho
        pesquisasFrequentes.put(palavra, pesquisasFrequentes.getOrDefault(palavra, 0) + 1);

        //simula atualização de tempo de resposta por Barrel (substituir pela lógica real) - VER ISSOOOOOO
        String barrel = "Barrel1";  // Aqui você pode associar um Barrel real da busca
        temposResposta.putIfAbsent(barrel, new ArrayList<>());
        temposResposta.get(barrel).add(duracao);

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

    // vai pra prox pag de pesquisa
    /*ve se ainda tem + resultados na lista de resultados da pesquisa, se tiver + (além da página atual), incrementa o nº da página e retorna os resultados dessa nova página. */
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

    //
    @Override
    public String previous_page() throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'previous_page'");
    }

    //links disponíveis em uma página de resultados
    @Override
    public List<String> links_to_page() throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'links_to_page'");
    }

    @Override
    public String pagina_estatisticas() throws RemoteException {
        StringBuilder relatorioEstatisticas = new StringBuilder();    //cria objeto StringBuilder p/ guardar o relatório das estatísticas -> diferente da string, deixa mudar o conteúdo sem criar novos objetos na memória

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
        relatorioEstatisticas.append("\nBarrels ativos e tamanho do índice:\n");  //título

        barrelsAtivos.forEach((barrel, tamanho) ->  // Percorre o mapa barrelsAtivos (Barrel -> tamanho do índice).
        relatorioEstatisticas.append(barrel)  // Adiciona o nome do Barrel.
                .append(": ")  // Adiciona um separador.
                .append(tamanho)  // Adiciona o tamanho do índice (número de URLs indexados).
                .append(" URLs\n"));  // Especifica que o valor representa URLs e pula para a próxima linha.

        //TEMPO MÉDIO de resposta por Barrel 
        relatorioEstatisticas.append("\nTempo médio de resposta por Barrel (décimos de segundo):\n");  //título

        for (var entry : temposResposta.entrySet()) {  // Percorre o mapa temposResposta (Barrel -> lista de tempos de resposta).
            String barrel = entry.getKey();  // Obtém o nome do Barrel.
            List<Long> tempos = entry.getValue();  // Obtém a lista de tempos de resposta para esse Barrel.

            long media = tempos.stream()  // Converte a lista de tempos de resposta em um fluxo de dados.
                            .mapToLong(Long::longValue)  // Converte a lista de objetos Long para primitivos long.
                            .sum() / tempos.size();  // Calcula a média somando todos os valores e dividindo pelo total.

            relatorioEstatisticas.append(barrel)  // Adiciona o nome do Barrel.
                .append(": ")  // Adiciona um separador.
                .append(media)  // Adiciona o tempo médio de resposta.
                .append("\n");  // Pula para a próxima linha.
        }

        return relatorioEstatisticas.toString();    //Retorna o relatório final como string
    }

    //sempre que indexar algo no barrel ele atualiza o tamanho
    public void atualizarTamanhoBarrel(String barrel, int tamanho) {
        barrelsAtivos.put(barrel, tamanho);
    }

    /* -> criei 3 maps (guardam chave e valor) p/ armazenar pesquisas frequentes, barrels ativos e tempos de resposta
    -> coloquei no pesquisar p/ atualizar estatísticas em tempo real
    -> pagina_estatisticas exibe os dados formatados
    -> criei o atualizarTamanhoBarrel p/ manter os barrels atualizados */
     
}