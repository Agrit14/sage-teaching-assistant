package com.sage.teachingassistant.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Manages storage and retrieval of generated and uploaded DOCX and PDF documents.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path storageDirectory;

    public FileStorageService(@Value("${sage.storage.path:./data/files}") String storagePath) {
        this.storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
            log.info("Sage document storage initialised at: {}", this.storageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory: " + storagePath, e);
        }
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }

    public Path resolveDocxPath(String runId, String suffix) {
        return storageDirectory.resolve(runId + "_" + sanitize(suffix) + ".docx");
    }

    public Path resolvePdfPath(String runId, String suffix) {
        return storageDirectory.resolve(runId + "_" + sanitize(suffix) + ".pdf");
    }

    public File saveDocx(String runId, String suffix, byte[] content) throws IOException {
        Path path = resolveDocxPath(runId, suffix);
        Files.write(path, content);
        log.info("Saved DOCX for run {} to {}", runId, path);
        return path.toFile();
    }

    public File savePdf(String runId, String suffix, byte[] content) throws IOException {
        Path path = resolvePdfPath(runId, suffix);
        Files.write(path, content);
        log.info("Saved PDF for run {} to {}", runId, path);
        return path.toFile();
    }

    public Optional<File> findDocx(String runId) {
        try (var stream = Files.list(storageDirectory)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith(runId) && p.getFileName().toString().endsWith(".docx"))
                    .findFirst()
                    .map(Path::toFile);
        } catch (IOException e) {
            log.error("Error listing files for run {}: {}", runId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<File> findPdf(String runId) {
        try (var stream = Files.list(storageDirectory)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith(runId) && p.getFileName().toString().endsWith(".pdf"))
                    .findFirst()
                    .map(Path::toFile);
        } catch (IOException e) {
            log.error("Error listing files for run {}: {}", runId, e.getMessage());
            return Optional.empty();
        }
    }

    private static String sanitize(String str) {
        if (str == null || str.isBlank()) {
            return "document";
        }
        return str.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
