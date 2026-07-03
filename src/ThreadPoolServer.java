import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Versao 3: multi-threaded com pool de threads (mesma ideia do slide 35 dos
 * slides 05.2: um ExecutorService com um numero fixo de threads recebe as
 * tarefas de atendimento).
 *
 * A diferenca pra versao 2 e que aqui o numero de threads em execucao
 * simultanea e limitado a POOL_SIZE. Se todas as threads do pool estiverem
 * ocupadas, as conexoes seguintes ficam esperando na fila interna do
 * ExecutorService (nao ficam disputando CPU/memoria como na versao anterior).
 */
public class ThreadPoolServer {

    private static final int POOL_SIZE = 20;

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        String dataFilePath = args.length > 1 ? args[1] : "server_data/records.txt";
        int poolSize = args.length > 2 ? Integer.parseInt(args[2]) : POOL_SIZE;

        FileRecordService fileService = new FileRecordService(Paths.get(dataFilePath));
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[thread-pool] ouvindo na porta " + port + " (pool de " + poolSize + " threads)");

            while (true) {
                Socket client = serverSocket.accept();
                pool.execute(new RequestHandler(client, fileService));
            }
        } finally {
            pool.shutdown();
        }
    }
}
