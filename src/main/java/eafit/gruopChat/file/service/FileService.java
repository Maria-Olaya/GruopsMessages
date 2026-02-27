package eafit.gruopChat.file.service;

import org.springframework.web.multipart.MultipartFile;
import eafit.gruopChat.file.dto.FileUploadResponseDTO;
import eafit.gruopChat.file.model.FileRecord;

public interface FileService {

    // Guarda el archivo en BD como Base64 y retorna la info para el mensaje
    FileUploadResponseDTO upload(MultipartFile file, Long uploaderId);

    // Recupera el FileRecord para servirlo como descarga
    FileRecord getFile(Long fileId);
}