package com.example.collaborative_editor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.regex.Pattern;

@Controller
public class PageController {

    private static final Pattern DOC_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    @GetMapping("/document/{docId}")
    public String getEditor(@PathVariable String docId) {
        if (!DOC_ID_PATTERN.matcher(docId).matches()) {
            return "redirect:/";
        }
        return "forward:/editor.html";
    }
}