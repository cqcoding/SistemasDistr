# Sistemas Distribuídos

**Projeto da disciplina de Sistemas Distribuídos - Universidade de Coimbra (2024/2025)**

## 👥 Equipe
- **Cecília Ernesto Silva Quaresma** - 2024245307
- **Bruna Dewes** - 2024243221
- **Heloísa Centenaro** - 2024246775

---
### Antes de iniciar:

O código está configurado para rodar em localhost, caso queira testar em duas máquinas diferentes, basta alterar o código `/src/config.properties` com o número de IP do computador que hospedará o servidor.

## Como rodar o projeto:

### Dentro da pasta `/src`, execute os arquivos na seguinte ordem:

1️⃣ **RegistrarBarrels.java** - Registra os barrels no sistema.  
2️⃣ **Servidor.java** - Inicia o servidor principal.  

### Para conectar o cliente:
3️⃣ **Execute Cliente.java** no terminal.

**Opções disponíveis no Cliente:**  
- `1️⃣` Pesquisar uma palavra já indexada.  
- `2️⃣` Indexar uma nova URL.  
- `3️⃣` Exibir estatísticas atualizadas em tempo real.  
- `4️⃣` Encerrar o cliente.  

### Para indexar novas URLs:
1. Após selecionar a opção `2️⃣` no cliente, execute em outro terminal:  
   - **WebCrawler.java**  
   - **Downloader.java**  

2. Isso garantirá que a lista de URLs indexadas seja atualizada no arquivo `urlIndexados.txt`.  

---

📌 **Observação:** Certifique-se de que todas as dependências necessárias estão instaladas antes de executar o projeto.  

