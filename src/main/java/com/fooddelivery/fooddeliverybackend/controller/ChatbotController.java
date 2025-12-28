package com.fooddelivery.fooddeliverybackend.controller;

import com.fooddelivery.fooddeliverybackend.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:3000") // Allow React frontend
public class ChatbotController {

    @Autowired
    private OpenAIService chatbotService;

    // DTO class for JSON request body
    public static class ChatRequest {
        public String query;
    }

    @PostMapping("/ask")
    public String askAI(@RequestBody ChatRequest request) {
        return chatbotService.getAIResponse(request.query);
    }
}
