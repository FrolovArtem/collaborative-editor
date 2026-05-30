package com.example.collaborative_editor.controller;

import com.example.collaborative_editor.service.DocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DocumentRestController {

    private final DocumentService documentService;

    public DocumentRestController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/api/documents")
    public List<String> listDocuments() {
        return documentService.getAllDocuments();
    }
}