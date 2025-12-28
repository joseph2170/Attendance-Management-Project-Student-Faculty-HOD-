package com.fooddelivery.controller;

import com.fooddelivery.fooddeliverybackend.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private OpenAIService openAIService;

    @PostMapping("/ask")
    public String askAI(@RequestParam String query) {
        return openAIService.getAIResponse(query);
    }
}
