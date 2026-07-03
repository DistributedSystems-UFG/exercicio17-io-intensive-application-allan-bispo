import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;

/**
 * Versao 2: multi-threaded, uma thread nova por requisicao.
 *
 * O accept() continua sendo feito em sequencia na thread principal (o loop
 * nao pode paralelizar isso), mas assim que a conexao chega, o atendimento
 * dela e delegado pra uma Thread nova, e o loop volta imediatamente pro
 * accept() da proxima conexao. Ou seja, N conexoes em andamento = N threads
 * vivas ao mesmo tempo, sem limite nenhum.
 */
public class ThreadPerRequestServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        String dataFilePath = args.length > 1 ? args[1] : "server_data/records.txt";

        FileRecordService fileService = new FileRecordService(Paths.get(dataFilePath));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[thread-per-request] ouvindo na porta " + port);

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new RequestHandler(client, fileService)).start();
            }
        }
    }
}
