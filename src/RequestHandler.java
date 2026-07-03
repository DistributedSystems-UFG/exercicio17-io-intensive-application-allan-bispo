import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Protocolo do servico (texto simples, uma requisicao por conexao):
 *
 *   cliente -> servidor: "GET <id>"
 *   servidor -> cliente: "OK <conteudo da linha>"  ou  "ERR <motivo>"
 *
 * Essa classe e o "trabalho" que cada uma das tres versoes do servidor
 * (single-thread, thread por requisicao, pool de threads) executa para
 * atender uma conexao. So muda COMO ela e chamada em cada versao.
 */
public class RequestHandler implements Runnable {

    private final Socket socket;
    private final FileRecordService fileService;

    public RequestHandler(Socket socket, FileRecordService fileService) {
        this.socket = socket;
        this.fileService = fileService;
    }

    @Override
    public void run() {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            String requestLine = in.readLine();
            if (requestLine == null) {
                return;
            }
            out.println(handle(requestLine));

        } catch (IOException e) {
            System.err.println("Erro ao atender cliente: " + e.getMessage());
        }
    }

    private String handle(String requestLine) {
        String[] parts = requestLine.trim().split("\\s+");
        if (parts.length != 2 || !parts[0].equalsIgnoreCase("GET")) {
            return "ERR BAD_REQUEST";
        }

        try {
            int recordId = Integer.parseInt(parts[1]);
            String record = fileService.readRecord(recordId);
            return (record != null) ? "OK " + record : "ERR NOT_FOUND";
        } catch (NumberFormatException e) {
            return "ERR BAD_REQUEST";
        } catch (IOException e) {
            return "ERR IO_ERROR";
        }
    }
}
