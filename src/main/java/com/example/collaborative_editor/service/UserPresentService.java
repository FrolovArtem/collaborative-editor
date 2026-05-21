package com.example.collaborative_editor.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserPresentService {
    private final Map<String, Set<String>> documentUsers = new ConcurrentHashMap<>();
    private final Map<String, UserSessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, String username) {
        if (sessionId == null || username == null) return;
        sessionInfoMap.put(sessionId, new UserSessionInfo(username, null));
    }

    public void moveToDocument(String sessionId, String docId) {
        if (sessionId == null || docId == null) return;
        UserSessionInfo info = sessionInfoMap.get(sessionId);
        if (info == null) return;
        if (info.docId != null && !info.docId.equals(docId)) {
            removeUserFromDocument(info.docId, info.username);
        }
        info.docId = docId;
        documentUsers.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(info.username);
    }

    public void removeSession(String sessionId) {
        if (sessionId == null) return;
        UserSessionInfo info = sessionInfoMap.remove(sessionId);
        if (info != null && info.docId != null) {
            removeUserFromDocument(info.docId, info.username);
        }
    }

    public Set<String> getUsersInDocument(String docId) {
        return documentUsers.getOrDefault(docId, Collections.emptySet());
    }

    public String getDocumentForSession(String sessionId) {
    UserSessionInfo info = sessionInfoMap.get(sessionId);
    return info != null ? info.docId : null;
}

    private void removeUserFromDocument(String docId, String username) {
        Set<String> users = documentUsers.get(docId);
        if (users != null) {
            users.remove(username);
            if (users.isEmpty()) {
                documentUsers.remove(docId);
            }
        }
    }

    private static class UserSessionInfo {

        final String username;
        String docId;

        UserSessionInfo(String username, String docId) {
            this.username = username;
            this.docId = docId;
        }
    }

}
