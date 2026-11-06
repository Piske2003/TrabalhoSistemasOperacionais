# Mini Sistema de Arquivos

Aplicação didática em Java 17+ e JavaFX para simular um sistema de arquivos em memória. Não cria nem apaga arquivos reais do computador.

## Requisitos
- JDK 17 ou superior
- Apache Maven 3.8+
- Conexão à internet na primeira execução para baixar dependências Maven

## Executar no Windows
1. Extraia a pasta do ZIP.
2. Abra `mini-sistema-arquivos`.
3. Dê dois cliques em `executar.bat` ou rode `mvn clean javafx:run` no terminal dessa pasta.

## Executar no IntelliJ IDEA
Abra a pasta como projeto Maven e execute `com.minifs.MainApp`. O JavaFX Maven Plugin configura as dependências necessárias.

## Executar no VS Code
Abra no VS Code a pasta `TrabalhoSistemasOperacionais` (ou diretamente
`mini-sistema-arquivos-corrigido`) e use `Terminal > Run Task... >
Executar Mini Sistema de Arquivos (JavaFX)`. A tarefa chama
`mvn javafx:run` dentro da pasta Maven, configurando o module-path e as
bibliotecas nativas do JavaFX.
Evite usar o botão genérico `Run Java` diretamente em `MainApp.java`, pois ele inicia
a JVM sem o module-path necessário para o JavaFX. Use a tarefa
`Executar Mini Sistema de Arquivos (JavaFX)` em `Terminal > Run Task...`, que executa
o Maven com o plugin JavaFX e inclui os módulos e as bibliotecas nativas necessários.

## Funcionalidades
- Árvore virtual de diretórios e arquivos.
- Criação de pastas e arquivos com tamanho em bytes.
- Disco virtual com 64 blocos de 1 KiB e bitmap de ocupação.
- Escolha da estratégia de alocação na tela inicial: encadeada, contígua ou indexada.
- Ao trocar a estratégia, o disco virtual é reiniciado, removendo pastas, arquivos e usuários.
- A desfragmentação não é exibida para a estratégia indexada, pois seus blocos não dependem de ordem contígua.
- Alocação contígua em sequência de blocos livres e alocação indexada com um bloco reservado para o índice.
- Exibição de caminho, tamanho e blocos alocados.
- Exclusão de arquivos/pastas, liberando recursivamente os blocos.
- Desfragmentação dos blocos ocupados, mantendo a ordem dos arquivos.
- Contadores de blocos livres/ocupados, arquivos e diretórios.
- Testes unitários do serviço de sistema de arquivos.
- Usuários identificados somente por nome e permissões de leitura, escrita e execução por arquivo.
- Permissão de exclusão por arquivo; somente usuários com essa permissão podem apagar arquivos.

## Limitações didáticas
O conteúdo dos arquivos não é armazenado: o tamanho informado serve para calcular os blocos necessários. Os dados existem somente enquanto a aplicação está aberta. A estratégia de alocação é uma simulação simples de alocação por blocos, não uma implementação de um sistema operacional real.

## Testes
`mvn test`
