package com.example.demo.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class OpenAiApiRequestDTO {
    private final String model;
    private final List<Message> messages;

    public OpenAiApiRequestDTO(String model, String prompt) {
        this.model = model;
        this.messages = List.of(new Message("user", prompt));
    }

    public OpenAiApiRequestDTO(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    @Getter
    public static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}