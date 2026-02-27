package eafit.gruopChat.file.dto;

// Lo que el backend devuelve tras un upload exitoso
// El frontend usa fileUrl para mandar el mensaje WebSocket
public record FileUploadResponseDTO(
        Long   fileId,
        String fileName,   // nombre original
        String mimeType,
        Long   sizeBytes,
        // La URL que el frontend mete en el mensaje — apunta al endpoint de descarga
        // Formato: /api/files/{fileId}
        String fileUrl
) {}