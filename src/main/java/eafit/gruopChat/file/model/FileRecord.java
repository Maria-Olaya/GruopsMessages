package eafit.gruopChat.file.model;

import java.time.LocalDateTime;

import eafit.gruopChat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "files")
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    // Quién subió el archivo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    // Nombre original del archivo (ej: "foto.jpg")
    @Column(name = "original_name", nullable = false)
    private String originalName;

    // Tipo MIME (ej: "image/jpeg", "application/pdf")
    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    // Tamaño en bytes
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    // Contenido completo en Base64
    // columnDefinition = "TEXT" para que H2/Hibernate no lo trunque
    @Column(name = "data_base64", nullable = false, columnDefinition = "TEXT")
    private String dataBase64;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getFileId() { return fileId; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getDataBase64() { return dataBase64; }
    public void setDataBase64(String dataBase64) { this.dataBase64 = dataBase64; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
}