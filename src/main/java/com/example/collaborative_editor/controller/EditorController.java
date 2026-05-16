package com.example.collaborative_editor.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class EditorController {

    private static final String DOCUMENTS_DIR = "D:/collaborative-editor/documents";

    private final Map<String, String> documents = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public EditorController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            Path dir = Paths.get(DOCUMENTS_DIR).toAbsolutePath().normalize();
            System.out.println("Рабочая директория: " + System.getProperty("user.dir"));
            System.out.println("Ожидаемая папка документов: " + dir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt")) {
                for (Path entry : stream) {
                    String docId = entry.getFileName().toString().replace(".txt", "");
                    String content = Files.readString(entry);
                    documents.put(docId, content);
                    System.out.println("Загружен документ: " + docId + " (длина " + content.length() + " символов)");
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка инициализации хранилища: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveToFile(String docId, String text) {
        try {
            Path dirPath = Paths.get(DOCUMENTS_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(docId + ".txt");
            Files.writeString(filePath, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения документа " + docId);
            e.printStackTrace();
        }
    }

    @MessageMapping("/edit/{docId}")
    @SendTo("/topic/document/{docId}")
    public String handleEdit(@DestinationVariable String docId, String message) {
        documents.put(docId, message);
        saveToFile(docId, message);
        System.out.println("Документ " + docId + " обновлён: " + message);
        return message;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        if (destination != null && destination.startsWith("/topic/document/")) {
            String docId = destination.substring("/topic/document/".length());
            String currentText = documents.getOrDefault(docId, "");
            System.out.println("Новый клиент в документе " + docId + ", отправляем текст: " + currentText);
            messagingTemplate.convertAndSend("/topic/document/" + docId, currentText);
        }
    }
}