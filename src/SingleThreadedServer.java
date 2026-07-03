import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;

/**
 * Versao 1: single-threaded.
 *
 * O servidor aceita uma conexao, atende ela por completo (le o arquivo, manda
 * a resposta, fecha o socket) e SO DEPOIS aceita a proxima. Nao existe nenhum
 * tipo de paralelismo: se a conexao N esta esperando o disco responder, a
 * conexao N+1 fica parada na fila de accept() sem nem comecar a ser atendida.
 */
public class SingleThreadedServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        String dataFilePath = args.length > 1 ? args[1] : "server_data/records.txt";

        FileRecordService fileService = new FileRecordService(Paths.get(dataFilePath));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[single-thread] ouvindo na porta " + port);

            while (true) {
                Socket client = serverSocket.accept();
                // chama o handler direto na thread principal: proxima conexao so
                // e aceita quando essa aqui terminar
                new RequestHandler(client, fileService).run();
            }
        }
    }
}
