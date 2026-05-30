package com.example.collaborative_editor.service;

import com.example.collaborative_editor.repository.DocumentFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private DocumentFileRepository fileRepository;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        fileRepository = mock(DocumentFileRepository.class);
        when(fileRepository.loadAll()).thenReturn(Collections.emptyMap());
        documentService = new DocumentService(fileRepository);
    }

    @Test
    void testGetDocumentTextEmpty() {
        assertEquals("", documentService.getDocumentText("nonexistent"));
    }

    @Test
    void testApplyPatchInsertsText() {
        List<Map<String, Object>> patches = new ArrayList<>();
        Map<String, Object> insert = new HashMap<>();
        insert.put("op", "insert");
        insert.put("pos", 0);
        insert.put("text", "Hello");
        patches.add(insert);

        documentService.applyPatch("test", patches);
        assertEquals("Hello", documentService.getDocumentText("test"));
        verify(fileRepository).save(eq("test"), eq("Hello"));
    }

    @Test
    void testApplyPatchDeleteText() {
        List<Map<String, Object>> insertPatch = List.of(
                Map.of("op", "insert", "pos", 0, "text", "Hello")
        );
        documentService.applyPatch("test", insertPatch);

        List<Map<String, Object>> deletePatch = List.of(
                Map.of("op", "delete", "pos", 0, "length", 1)
        );
        documentService.applyPatch("test", deletePatch);
        assertEquals("ello", documentService.getDocumentText("test"));
    }

    @Test
    void testGetAllDocuments() {
        documentService.applyPatch("doc1", List.of(Map.of("op", "insert", "pos", 0, "text", "A")));
        documentService.applyPatch("doc2", List.of(Map.of("op", "insert", "pos", 0, "text", "B")));
        List<String> docs = documentService.getAllDocuments();
        assertTrue(docs.contains("doc1"));
        assertTrue(docs.contains("doc2"));
    }
}