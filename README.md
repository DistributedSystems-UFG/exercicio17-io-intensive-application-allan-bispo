[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/sMLHUOxM)

# Exercicio 17 - I/O Intensive Application

Servico de arquivos acessivel via rede, implementado em Java com socket TCP,
em tres versoes:

1. `SingleThreadedServer` - single-threaded, atende uma conexao por vez.
2. `ThreadPerRequestServer` - cria uma thread nova pra cada requisicao.
3. `ThreadPoolServer` - usa um pool fixo de threads (`ExecutorService`) pra
   atender as requisicoes.

As tres compartilham o mesmo protocolo e a mesma logica de leitura do
arquivo (`RequestHandler` e `FileRecordService`), a unica coisa que muda
entre elas e a forma como cada conexao aceita e' entregue pra ser
processada.

## Protocolo

Texto simples, uma requisicao por conexao:

```
cliente -> servidor: GET <id>
servidor -> cliente: OK <conteudo da linha>
             ou      ERR NOT_FOUND / ERR BAD_REQUEST / ERR IO_ERROR
```

`<id>` e o numero da linha (a partir de 1) do arquivo `server_data/records.txt`,
que faz o papel dos "dados" que o servico expoe.

## Estrutura

```
src/
  FileRecordService.java     -> le uma linha do arquivo (I/O de verdade, sem cache)
  RequestHandler.java        -> parseia "GET <id>" e monta a resposta
  SingleThreadedServer.java  -> versao 1
  ThreadPerRequestServer.java-> versao 2
  ThreadPoolServer.java      -> versao 3
  Client.java                -> cliente simples pra testar na mao
  LoadTester.java            -> gerador de carga usado na comparacao de vazao
server_data/
  records.txt                -> arquivo de dados (2000 linhas)
```

## Como compilar

```bash
javac -d out src/*.java
```

## Como rodar

Cada versao roda numa porta (padrao 8000). So dar `Ctrl+C` pra derrubar.

```bash
# versao 1
java -cp out SingleThreadedServer 8000 server_data/records.txt

# versao 2
java -cp out ThreadPerRequestServer 8001 server_data/records.txt

# versao 3 (pool com 20 threads por padrao)
java -cp out ThreadPoolServer 8002 server_data/records.txt 20
```

Testando na mao com o cliente:

```bash
java -cp out Client localhost 8000 42
# OK registro 42: dado de exemplo armazenado no servidor (linha 42 do arquivo)
```

## Como medir a vazao

O `LoadTester` dispara N clientes concorrentes, cada um abrindo uma conexao
por requisicao (do jeito que o protocolo pede) e pedindo registros
aleatorios, ate completar `clientes x requisicoesPorCliente` requisicoes.
No final ele imprime total de requisicoes, tempo gasto, vazao (req/s) e
latencia media.

```bash
java -cp out LoadTester localhost 8000 50 100 2000
#   host=localhost porta=8000 50 clientes concorrentes, 100 requisicoes cada, ids de 1 a 2000
```

Pra comparar as tres versoes: subir cada servidor numa porta diferente e
rodar o mesmo comando de `LoadTester` (mesmos parametros) contra cada uma,
uma de cada vez (senao elas competem pelos mesmos recursos da maquina e o
numero fica sujo). Ver `COMPARACAO.md` pra analise dos resultados.
