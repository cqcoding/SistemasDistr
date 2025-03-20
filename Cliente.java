import java.rmi.Naming;
import java.util.List;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) throws Exception {
        try {
            // Conectar ao GatewayServer via RMI
            String objName = "rmi://192.168.1.164/server";      //quando for em outra maquina põe o ip ao invés do localhost
            InterfaceGatewayServer gateway = (InterfaceGatewayServer) Naming.lookup(objName);     //o cliente precisa estar ciente dos métodos da interface por isso chamamos a interface
            
            Scanner scanner = new Scanner(System.in);
            
            while (true) {            //enquanto não clicar no sair fica rodando
                System.out.println("Cliente iniciado! \nEscolha a opção: \n1 - Para Pesquisar \n2 - Para Indexar \n3 - Ver as estatísticas \n4 - Para sair");

                int opcao = scanner.nextInt();
                scanner.nextLine();              //quebra de linha

                if (opcao == 1){
                    System.out.println("Digite sua pesquisa:  ");
                    String palavra = scanner.nextLine();

                    List<String> resultados = gateway.pesquisar(palavra);    //faz lista de resultados e chama a função pesquisar
                
                    if (resultados.isEmpty()) {          //se a lista estiver vazia vai dizer que não encontrou
                        System.out.println("Nenhum resultado encontrado.");
                    } 

                    else {     //se não, vai printar os resultados
                        System.out.println("Resultados:");
                       
                        for (String url : resultados) {             //percorre todas as urls armazenadas em 'resultados'
                            System.out.println("- " + url);           //printa elas
                        }
                    }    
                }

                else if (opcao == 2){
                    System.out.println("URL para indexar:  ");
                    String url = scanner.nextLine();

                    gateway.indexar_URL(url);
                    System.out.println("URL indexada com sucesso!");      
                }

                else if (opcao == 3){
                    gateway.pagina_estatisticas();
                    System.out.println("Estatísticas mostradas com sucesso!");      
                }

                else if (opcao == 3){
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