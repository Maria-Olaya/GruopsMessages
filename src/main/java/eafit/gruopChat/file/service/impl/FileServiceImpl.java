package eafit.gruopChat.file.service.impl;

import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import eafit.gruopChat.file.dto.FileUploadResponseDTO;
import eafit.gruopChat.file.exception.FileNotFoundException;
import eafit.gruopChat.file.exception.FileTooLargeException;
import eafit.gruopChat.file.exception.InvalidFileException;
import eafit.gruopChat.file.model.FileRecord;
import eafit.gruopChat.file.repository.FileRepository;
import eafit.gruopChat.file.service.FileService;
import eafit.gruopChat.user.exception.UserNotFoundException;
import eafit.gruopChat.user.model.User;
import eafit.gruopChat.user.repository.UserRepository;

@Service
@Transactional
public class FileServiceImpl implements FileService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public FileServiceImpl(FileRepository fileRepository, UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    @Override
    public FileUploadResponseDTO upload(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("el archivo está vacío");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new FileTooLargeException(MAX_SIZE_BYTES);
        }

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new UserNotFoundException(uploaderId));

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new InvalidFileException("no se pudo leer el archivo");
        }

        String base64 = Base64.getEncoder().encodeToString(bytes);

        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        String originalName = sanitizeName(file.getOriginalFilename());

        FileRecord record = new FileRecord();
        record.setUploadedBy(uploader);
        record.setOriginalName(originalName);
        record.setMimeType(mimeType);
        record.setSizeBytes(file.getSize());
        record.setDataBase64(base64);

        FileRecord saved = fileRepository.save(record);

        return new FileUploadResponseDTO(
                saved.getFileId(),
                saved.getOriginalName(),
                saved.getMimeType(),
                saved.getSizeBytes(),
                "/api/files/" + saved.getFileId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FileRecord getFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    private String sanitizeName(String name) {
        if (name == null || name.isBlank()) return "archivo";
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}