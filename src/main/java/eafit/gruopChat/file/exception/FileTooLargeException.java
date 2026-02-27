package eafit.gruopChat.file.exception;

public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(long maxBytes) {
        super("El archivo supera el límite permitido de " + (maxBytes / 1024 / 1024) + " MB");
    }
}