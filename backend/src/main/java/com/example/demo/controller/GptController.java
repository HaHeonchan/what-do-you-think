package com.example.demo.controller;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/gpt", produces = MediaType.APPLICATION_JSON_VALUE)
public class GptController {

    private final ChatService chatService;

    public GptController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/question", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponseDTO> sendQuestion(@RequestBody ChatRequestDTO requestDto) {
        try {
            ChatResponseDTO response = chatService.askQuestion(requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponseDTO("오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/summarize")
    public ResponseEntity<ChatResponseDTO> summarizeConversation() {
        try {
            ChatResponseDTO response = chatService.summarizeConversation();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponseDTO("요약 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}