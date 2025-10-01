package com.example.demo.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class OpenAiApiRequestDTO {
    private final String model;
    private final List<Message> messages;
    private final int max_output_tokens;

    public OpenAiApiRequestDTO(String model, String prompt, Integer max_tokens) {
        this.model = model;
        this.messages = List.of(new Message("user", prompt));
        this.max_output_tokens = max_tokens;
    }

    public OpenAiApiRequestDTO(String model, List<Message> messages, Integer max_tokens) {
        this.model = model;
        this.messages = messages;
        this.max_output_tokens = max_tokens;
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