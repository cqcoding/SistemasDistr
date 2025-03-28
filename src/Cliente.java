import java.io.InputStream;
import java.rmi.Naming;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        try {
            // Carregar propriedades usando o ClassLoader
            Properties properties = new Properties();
            try (InputStream input = Cliente.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    System.out.println("Desculpe, não foi possível encontrar config.properties");
                    return;
                }
                properties.load(input);
            }

            // Obter o IP do servidor a partir das propriedades
            String serverIp = properties.getProperty("server.ip", "localhost");
            String server = "rmi://" + serverIp + "/server";

            InterfaceGatewayServer gateway = (InterfaceGatewayServer) Naming.lookup(server);
            
            Scanner scanner = new Scanner(System.in);
            
            while (true) {            //enquanto não clicar no sair fica rodando
                System.out.println("\n \nEscolha a opção: \n1 - Para Pesquisar \n2 - Para Indexar \n3 - Ver as estatísticas \n4 - Para sair");

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

                    gateway.enviarURLParaProcessamento(url);       //adiciona url à lista de processamento dos barrels
                    System.out.println("URL indexada com sucesso!");      
                }

                else if (opcao == 3){
                    String estatisticas = gateway.pagina_estatisticas();  //pega as estatísticas
                    System.out.println(estatisticas);                     //e imprime no cliente
                    
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