package eafit.gruopChat.file.exception;

public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String reason) {
        super("Archivo inválido: " + reason);
    }
}