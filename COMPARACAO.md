# Comparacao de vazao - single-thread x thread por requisicao x pool de threads

Pra comparar as tres versoes eu usei o `LoadTester` que fiz (`src/LoadTester.java`),
rodando localhost mesmo, um servidor de cada vez (senao os tres iam brigar por CPU
e o numero ia sair mentiroso). O arquivo de dados tem 2000 linhas
(`server_data/records.txt`) e cada requisicao pede uma linha aleatoria entre 1 e 2000,
abrindo uma conexao nova por requisicao (e como o protocolo funciona).

O pool de threads ficou fixado em 20 threads (`ThreadPoolServer`, igual o exemplo do
slide 35).

Testei em tres niveis de concorrencia (clientes simultaneos disparando requisicao),
sempre 100 requisicoes por cliente:

| Clientes concorrentes | Total de requisicoes | Single-thread (req/s) | Thread por requisicao (req/s) | Pool de threads - 20 (req/s) |
|---|---|---|---|---|
| 10  | 1.000  | ~480 | ~1.800 | ~1.750 |
| 50  | 5.000  | ~460 | ~3.200 | ~3.400 |
| 200 | 20.000 | ~450 | ~2.600 | ~3.500 |

(numeros de uma rodada representativa; variam um pouco de execucao pra execucao,
mas o padrao entre as tres versoes se manteve nas rodadas que eu fiz.)

## O que da pra tirar disso

**Single-thread** e sempre a pior, e o pior de um jeito bem "burro": a vazao dela
nem muda muito com o numero de clientes concorrentes, porque nao importa quantos
clientes estao tentando falar com o servidor ao mesmo tempo - ele so processa um
por vez. Enquanto uma conexao esta esperando o disco responder, todas as outras
ficam paradas na fila do `accept()` sem nem comecar a ser atendidas. A vazao dela
e basicamente `1 / (tempo medio por requisicao)`, e isso e um teto que nao sobe.

**Thread por requisicao** ja da um salto grande em relacao a single-thread, porque
agora as esperas de I/O de varias conexoes acontecem ao mesmo tempo (enquanto uma
thread esta bloqueada esperando o disco, outras threads continuam trabalhando).
O problema aparece quando a concorrencia fica alta (200 clientes): a vazao cai em
vez de continuar subindo. Isso e porque criar uma thread do SO tem custo (alocacao
de pilha, registro no escalonador), e com centenas de threads vivas ao mesmo tempo o
overhead de troca de contexto e o consumo de memoria comecam a comer o ganho que a
concorrencia deveria trazer. Ou seja, essa versao nao tem protecao nenhuma contra
sobrecarga: ela aceita quantas conexoes vierem e cria uma thread pra cada uma, sem
limite.

**Pool de threads** foi a que teve o melhor comportamento no geral. Com poucos
clientes ela fica parecida com a versao de thread por requisicao (faz sentido,
porque com 10-50 clientes concorrentes as 20 threads do pool praticamente ja dao
conta sem fila se formar). A diferenca aparece com 200 clientes: enquanto a
thread-por-requisicao degrada, o pool se mantem estavel e ainda tem a maior vazao
das tres. As conexoes que chegam alem da capacidade das 20 threads ficam numa fila
interna do `ExecutorService` esperando uma thread liberar, em vez de virar mais uma
thread disputando CPU/memoria com as demais. Isso limita o custo de troca de
contexto e deixa o servidor previsivel mesmo sob carga alta - o que faz sentido
com o que foi discutido nos slides 05.2 sobre pool de threads.

## Conclusao

A ordem de vazao foi: **pool de threads >= thread por requisicao > single-thread**,
com a distancia entre pool e thread-por-requisicao crescendo conforme a
concorrencia aumenta (o pool escala melhor sob carga alta porque limita quantas
threads existem ao mesmo tempo, em vez de deixar esse numero crescer sem controle).
Isso bate com o que da pra esperar teoricamente: como a operacao do servico e
dominada por I/O (leitura do arquivo), qualquer coisa que permita sobrepor essas
esperas ja melhora bastante a vazao em cima da versao single-thread; e limitar
quantas dessas esperas acontecem em paralelo (pool) evita que o custo de gerenciar
threads demais vire o novo gargalo.
