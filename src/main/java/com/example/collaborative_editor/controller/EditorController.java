package com.example.collaborative_editor.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class EditorController {

    private final Map<String, String> documents = new ConcurrentHashMap<>();

    @MessageMapping("/edit/{docId}")
    @SendTo("/topic/document/{docId}")
    public String handleEdit(@DestinationVariable String docId, String message) {
        documents.put(docId, message);
        System.out.println("Документ " + docId + " обновлён: " + message);
        return message;
    }

    @SubscribeMapping("/topic/document/{docId}")
    public String onSubscribe(@DestinationVariable String docId) {
        String currentText = documents.getOrDefault(docId, "");
        System.out.println("Новый клиент в документе "+ docId + ", отправляем текст: " + currentText);
        return currentText;
    }
}
