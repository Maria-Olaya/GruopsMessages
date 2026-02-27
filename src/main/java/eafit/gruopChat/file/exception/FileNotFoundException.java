package eafit.gruopChat.file.exception;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(Long fileId) {
        super("Archivo no encontrado: " + fileId);
    }
}