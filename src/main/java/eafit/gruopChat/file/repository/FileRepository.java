package eafit.gruopChat.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import eafit.gruopChat.file.model.FileRecord;

public interface FileRepository extends JpaRepository<FileRecord, Long> {
    // Por ahora solo necesitamos findById (heredado) y save
}