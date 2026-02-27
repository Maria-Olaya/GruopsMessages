package eafit.gruopChat.file.controller;

import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import eafit.gruopChat.file.dto.FileUploadResponseDTO;
import eafit.gruopChat.file.model.FileRecord;
import eafit.gruopChat.file.service.FileService;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // POST /api/files/upload
    // El frontend manda el archivo como multipart/form-data con el campo "file"
    // Devuelve { fileId, fileName, mimeType, sizeBytes, fileUrl }
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponseDTO> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) {
        FileUploadResponseDTO response = fileService.upload(file, userId);
        return ResponseEntity.ok(response);
    }

    // GET /api/files/{fileId}
    // Sirve el archivo con el Content-Type correcto para que el browser lo muestre
    // Las imágenes se renderizan inline, los PDFs/docs se descargan
    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> download(@PathVariable Long fileId) {
        FileRecord record = fileService.getFile(fileId);

        byte[] bytes = Base64.getDecoder().decode(record.getDataBase64());

        // Determinar si mostrarlo inline o forzar descarga
        String disposition = isInlineType(record.getMimeType())
                ? "inline"
                : "attachment; filename=\"" + record.getOriginalName() + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CONTENT_TYPE, record.getMimeType())
                .body(bytes);
    }

    // Las imágenes y PDFs se muestran inline en el browser/chat
    private boolean isInlineType(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.startsWith("image/") || mimeType.equals("application/pdf");
    }
}