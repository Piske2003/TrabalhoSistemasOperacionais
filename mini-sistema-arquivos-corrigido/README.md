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

## Funcionalidades
- Árvore virtual de diretórios e arquivos.
- Criação de pastas e arquivos com tamanho em bytes.
- Disco virtual com 64 blocos de 1 KiB e bitmap de ocupação.
- Alocação de blocos livres em ordem crescente.
- Exibição de caminho, tamanho e blocos alocados.
- Exclusão de arquivos/pastas, liberando recursivamente os blocos.
- Contadores de blocos livres/ocupados, arquivos e diretórios.
- Testes unitários do serviço de sistema de arquivos.

## Limitações didáticas
O conteúdo dos arquivos não é armazenado: o tamanho informado serve para calcular os blocos necessários. Os dados existem somente enquanto a aplicação está aberta. A estratégia de alocação é uma simulação simples de alocação por blocos, não uma implementação de um sistema operacional real.

## Testes
`mvn test`
