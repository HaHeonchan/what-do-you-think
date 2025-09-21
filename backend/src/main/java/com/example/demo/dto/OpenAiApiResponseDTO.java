package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class OpenAiApiResponseDTO {
    private List<Choice> choices;

    @Getter
    @NoArgsConstructor
    public static class Choice {
        private Message message;
    }

    @Getter
    @NoArgsConstructor
    public static class Message {
        private String content;
    }
}