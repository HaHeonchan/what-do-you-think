package com.example.demo.controller;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.service.GptService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gpt")
public class GptController {

    private final GptService gptService;

    public GptController(GptService gptService) {
        this.gptService = gptService;
    }

    @PostMapping("/question")
    public ChatResponseDTO sendQuestion(@RequestBody ChatRequestDTO requestDto) {
        return gptService.askQuestion(requestDto);
    }
}