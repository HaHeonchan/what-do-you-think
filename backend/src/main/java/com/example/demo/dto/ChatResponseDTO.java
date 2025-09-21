package com.example.demo.dto;

import lombok.Getter;

@Getter
public class ChatResponseDTO {
    private final String answer;

    public ChatResponseDTO(String answer) {
        this.answer = answer;
    }
}