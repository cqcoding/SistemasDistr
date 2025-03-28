import java.rmi.Naming;
import java.util.List;
import java.util.Scanner;

/**
 * Cliente RMI para interação com o GatewayServer.
 * Permite pesquisar URLs indexadas, enviar URLs para indexação e visualizar estatísticas do servidor, opções dispostas em um menu.
 */
public class Cliente {
    /**
     * Método principal que executa o cliente.
     * Conecta ao servidor via RMI e permite ao usuário interagir com as funcionalidades disponíveis.
     *
     * @throws Exception -> caso ocorrer um erro de comunicação RMI.
     */
    public static void main(String[] args) throws Exception {
        try {
            String server = "rmi://192.168.1.164/server";      // note: quando for em outra máquina, colocar o IP ao invés do localhost.
            InterfaceGatewayServer gateway = (InterfaceGatewayServer) Naming.lookup(server);     // o cliente precisa estar ciente dos métodos da interface, por isso a interface é chamada.
            
            Scanner scanner = new Scanner(System.in);
            
            while (true) {            // loop do menu interativo - enquanto não escolher a opção de sair ficará compilando.
                System.out.println("\n \nEscolha a opção: \n1 - Para Pesquisar \n2 - Para Indexar \n3 - Ver as estatísticas \n4 - Para sair");

                int opcao = scanner.nextInt();
                scanner.nextLine();             

                if (opcao == 1){
                    System.out.println("Digite sua pesquisa:  ");
                    String palavra = scanner.nextLine();

                    List<String> resultados = gateway.pesquisar(palavra);    // cria a lista de resultados e chama a função pesquisar.
                
                    if (resultados.isEmpty()) {          // se a lista estiver vazia, irá aparecer a mensagem informando que não encontrou resultado.
                        System.out.println("Nenhum resultado encontrado.");
                    } 

                    else {     // se não, irá printar os resultados.
                        System.out.println("Resultados:");
                       
                        for (String url : resultados) {             // percorre todas as URLs armazenadas em 'resultados'.
                            System.out.println("- " + url);
                        }
                    }    
                }

                else if (opcao == 2){
                    System.out.println("URL para indexar:  ");
                    String url = scanner.nextLine();

                    gateway.enviarURLParaProcessamento(url);       // adiciona a URL à lista de processamento dos Barrels.
                    System.out.println("URL indexada com sucesso!");      
                }

                else if (opcao == 3){
                    String estatisticas = gateway.pagina_estatisticas();  // coleta as estatísticas.
                    System.out.println(estatisticas);                    
                    
                    System.out.println("Estatísticas mostradas com sucesso!");      
                }

                else if (opcao == 4){
                    System.out.println("Encerra cliente");
                    scanner.close();
                    break;                  
                }
                else {
                    System.out.println ("Opcao invalida. Tente novamente.");
                }
            } 
        } 
        catch (Exception e){
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }

    }
    
}