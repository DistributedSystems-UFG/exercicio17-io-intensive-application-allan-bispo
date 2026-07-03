import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Cliente simples so pra testar manualmente qualquer uma das tres versoes
 * do servidor. Uso: java Client [host] [porta] [idDoRegistro]
 */
public class Client {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8000;
        int recordId = args.length > 2 ? Integer.parseInt(args[2]) : 1;

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET " + recordId);
            System.out.println(in.readLine());
        }
    }
}
