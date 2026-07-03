import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

/**
 * Le um "registro" (uma linha) do arquivo de dados do servidor, dado o seu numero.
 * De proposito, o arquivo e reaberto e varrido a cada chamada (sem cache em memoria),
 * pra deixar a operacao realmente presa a I/O de disco, que e o ponto de comparacao
 * deste exercicio.
 */
public class FileRecordService {

    private final Path dataFile;

    public FileRecordService(Path dataFile) {
        this.dataFile = dataFile;
    }

    /**
     * @param recordId numero da linha (comecando em 1)
     * @return o conteudo da linha, ou null se o registro nao existir
     */
    public String readRecord(int recordId) throws IOException {
        if (recordId < 1) {
            return null;
        }
        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "r")) {
            String line;
            int current = 0;
            while ((line = raf.readLine()) != null) {
                current++;
                if (current == recordId) {
                    return line;
                }
            }
        }
        return null;
    }
}
