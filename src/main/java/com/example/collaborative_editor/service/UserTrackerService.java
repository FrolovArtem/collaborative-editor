package com.example.collaborative_editor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.*;

@Service
public class UserTrackerService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresentService userPresentService;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserTrackerService(SimpMessagingTemplate messagingTemplate,
                              UserPresentService userPresentService,
                              DocumentService documentService) {
        this.messagingTemplate = messagingTemplate;
        this.userPresentService = userPresentService;
        this.documentService = documentService;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getFirstNativeHeader("username");
        String sessionId = accessor.getSessionId();
        userPresentService.registerSession(sessionId, username);
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        if (destination == null || sessionId == null) return;

        String docId = extractDocId(destination);
        if (docId == null) return;

        userPresentService.moveToDocument(sessionId, docId);
        sendUserList(docId);

        if (destination.equals("/topic/document/" + docId)) {
            String currentText = documentService.getDocumentText(docId);
            if (!currentText.isEmpty()) {
                List<Map<String, Object>> initPatch = new ArrayList<>();
                Map<String, Object> insert = new HashMap<>();
                insert.put("op", "insert");
                insert.put("pos", 0);
                insert.put("text", currentText);
                initPatch.add(insert);
                try {
                    String json = objectMapper.writeValueAsString(initPatch);
                    messagingTemplate.convertAndSendToUser(sessionId, "/topic/document/" + docId, json);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String docId = userPresentService.getDocumentForSession(sessionId);
        userPresentService.removeSession(sessionId);
        if (docId != null) {
            sendUserList(docId);
        }
    }

    private String extractDocId(String destination) {
        if (destination.startsWith("/topic/document/")) {
            String subPath = destination.substring("/topic/document/".length());
            String[] parts = subPath.split("/");
            if (parts.length >= 1) {
                return parts[0];
            }
        }
        return null;
    }

    private void sendUserList(String docId) {
        Set<String> users = userPresentService.getUsersInDocument(docId);
        List<String> userList = new ArrayList<>(users);
        try {
            String json = objectMapper.writeValueAsString(userList);
            messagingTemplate.convertAndSend("/topic/document/" + docId + "/users", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}