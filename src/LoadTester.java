import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gerador de carga usado para medir a vazao (requisicoes/segundo) de cada
 * uma das tres versoes do servidor.
 *
 * Simula N clientes concorrentes, cada um abrindo uma conexao nova por
 * requisicao (igual o protocolo do servidor espera) e pedindo registros
 * aleatorios do arquivo. No final imprime total de requisicoes, tempo
 * gasto, vazao e latencia media.
 *
 * Uso: java LoadTester [host] [porta] [clientesConcorrentes] [requisicoesPorCliente] [maiorIdDoRegistro]
 */
public class LoadTester {

    public static void main(String[] args) throws InterruptedException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8000;
        int numClients = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int requestsPerClient = args.length > 3 ? Integer.parseInt(args[3]) : 100;
        int maxRecordId = args.length > 4 ? Integer.parseInt(args[4]) : 2000;

        ExecutorService clientPool = Executors.newFixedThreadPool(numClients);
        CountDownLatch latch = new CountDownLatch(numClients);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicLong totalLatencyNanos = new AtomicLong();

        System.out.printf("Disparando %d clientes x %d requisicoes (%d requisicoes no total) contra %s:%d%n",
                numClients, requestsPerClient, numClients * requestsPerClient, host, port);

        long start = System.nanoTime();

        for (int i = 0; i < numClients; i++) {
            clientPool.submit(() -> {
                Random random = new Random();
                try {
                    for (int r = 0; r < requestsPerClient; r++) {
                        int recordId = 1 + random.nextInt(maxRecordId);
                        long reqStart = System.nanoTime();
                        boolean ok = doRequest(host, port, recordId);
                        totalLatencyNanos.addAndGet(System.nanoTime() - reqStart);
                        if (ok) {
                            success.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long elapsedNanos = System.nanoTime() - start;

        clientPool.shutdown();
        clientPool.awaitTermination(5, TimeUnit.SECONDS);

        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        int totalOk = success.get();
        int totalRequests = totalOk + failed.get();
        double throughput = totalOk / elapsedSeconds;
        double avgLatencyMs = (totalRequests > 0)
                ? (totalLatencyNanos.get() / 1_000_000.0) / totalRequests
                : 0;

        System.out.println("----------------------------------------");
        System.out.printf("Requisicoes totais : %d (sucesso=%d, falha=%d)%n", totalRequests, totalOk, failed.get());
        System.out.printf("Tempo total        : %.2f s%n", elapsedSeconds);
        System.out.printf("Vazao              : %.2f req/s%n", throughput);
        System.out.printf("Latencia media     : %.2f ms%n", avgLatencyMs);
    }

    private static boolean doRequest(String host, int port, int recordId) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET " + recordId);
            String response = in.readLine();
            return response != null && response.startsWith("OK");
        } catch (IOException e) {
            return false;
        }
    }
}
