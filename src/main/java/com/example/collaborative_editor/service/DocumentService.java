package com.example.collaborative_editor.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.collaborative_editor.repository.DocumentFileRepository;

import jakarta.annotation.PostConstruct;

@Service
public class DocumentService {
    
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private final DocumentFileRepository fileRepository;
    private final Map<String, String> documents = new ConcurrentHashMap<>();

    public DocumentService(DocumentFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @PostConstruct
    public void init() {
        Map<String, String> loaded = fileRepository.loadAll();
        documents.putAll(loaded);
        System.out.println("Загружено документов: " + documents.size());
    }

    public String getDocumentText(String docId) {
        return documents.getOrDefault(docId, "");
    }

    public void updateDocument(String docId, String text) {
        documents.put(docId, text);
        fileRepository.save(docId, text);
        System.out.println("Документ " + docId + " обновлён");
    }

    public String applyPatch(String docId, List<Map<String, Object>> patches) {
        Object lock = locks.computeIfAbsent(docId, k -> new Object());
        synchronized (lock) {
            String text = documents.getOrDefault(docId, "");
            StringBuilder sb = new StringBuilder(text);
            int shift = 0;
            for (Map<String, Object> patch : patches) {
                String op = (String) patch.get("op");
                int pos = (int) patch.get("pos") + shift;
                if (pos < 0) pos = 0;
                if (pos > sb.length()) pos = sb.length();

                if ("insert".equals(op)) {
                    String insertText = (String) patch.get("text");
                    sb.insert(pos, insertText);
                    shift += insertText.length();
                } else if ("delete".equals(op)) {
                    int length = (int) patch.get("length");
                    int end = pos + length;
                    if (end > sb.length()) {
                        length = sb.length() - pos;
                        end = sb.length();
                    }
                    if (length > 0) {
                        sb.delete(pos, end);
                        shift -= length;
                    }
                }
            }
            String newText = sb.toString();
            documents.put(docId, newText);
            fileRepository.save(docId, newText);
            System.out.println("Патч применён к документу " + docId);
            return newText;
        }   
    }

    public List<String> getAllDocuments() {
        return new ArrayList<>(documents.keySet());
    }
    
}
