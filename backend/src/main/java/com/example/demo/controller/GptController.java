package com.example.demo.controller;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gpt")
public class GptController {

    private final ChatService chatService;

    public GptController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/question")
    public ChatResponseDTO sendQuestion(@RequestBody ChatRequestDTO requestDto) {
        return chatService.askQuestion(requestDto);
    }
}