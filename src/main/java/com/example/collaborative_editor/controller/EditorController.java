package com.example.collaborative_editor.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.example.collaborative_editor.service.DocumentService;

import java.util.List;
import java.util.Map;

@Controller
public class EditorController {

    private final DocumentService documentService;

    public EditorController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @MessageMapping("/edit/{docId}")
    @SendTo("/topic/document/{docId}")
    public String handleEdit(@DestinationVariable String docId, String message) {
        documentService.updateDocument(docId, message);
        return message;
    }

    @MessageMapping("/patch/{docId}")
    @SendTo("/topic/document/{docId}")
    public List<Map<String, Object>> handlePatch(@DestinationVariable String docId, List<Map<String, Object>> patches) {
        System.out.println("Получен патч для документа " + docId + ": " + patches);
        documentService.applyPatch(docId, patches);
        return patches;
    }
}