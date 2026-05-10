package com.example.collaborative_editor.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class EditorController {

    @MessageMapping("/edit")
    @SendTo("/topic/document")

    public String handleEdite (String message) {
        return message;
    }
}
