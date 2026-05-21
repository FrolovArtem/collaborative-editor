package com.example.collaborative_editor.repository;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentFileRepository {
    
    @Value("${documents.storage-path}")
    private String storagePath;

    private Path getDirPath() {
        return Paths.get(storagePath).toAbsolutePath().normalize();
    }

    public Map<String, String> loadAll() {
        Map<String, String> docs = new HashMap<>();
        try {
            Path dir = getDirPath();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            } 
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt")) {
                for (Path entry : stream) {
                    String docId = entry.getFileName().toString().replace(".txt", "");
                    String content = Files.readString(entry);
                    docs.put(docId, content);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки документов " + e.getMessage());
        }
        return docs;
    }

    public void save(String docId, String text) {
        try {
            Path dir = getDirPath();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path filPath = dir.resolve(docId + ".txt");
            Files.writeString(filPath, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения документа " + docId + ": " + e.getMessage());
        }
    }
}
