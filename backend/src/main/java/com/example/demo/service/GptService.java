package com.example.demo.service;

import com.example.demo.entity.ChatEntity;

// 1. Spring AI (1.0.3 API)의 올바른 import 경로
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GptService {
    private final OpenAiChatModel openAiChatModel;

    @Autowired
    public GptService(OpenAiChatModel openAiChatModel) {
        this.openAiChatModel = openAiChatModel;
    }


    @Transactional
    public ChatEntity requestGpt(List<Map<String, String>> messages, String senderRole) {

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-5-nano-2025-08-07")
                .temperature(0.4)
                .build();

        // 1. ChatService의 List<Map>을 Spring AI의 List<Message>로 변환
        List<Message> springAiMessages = messages.stream()
                .map(msgMap -> {
                    String role = msgMap.get("role");
                    String content = msgMap.get("content");
                    switch (role) {
                        case "system":
                            return new SystemMessage(content);
                        case "user":
                            return new UserMessage(content);
                        case "assistant":
                            return new AssistantMessage(content);
                        default:
                            return new UserMessage(content);
                    }
                })
                .collect(Collectors.toList());

        // 2. Spring AI의 Prompt 객체 생성
        // application.yml의 model, temperature, max-tokens 설정이
        // OpenAiChatModel에 의해 자동으로 적용됩니다.
        Prompt prompt = new Prompt(springAiMessages);

        ChatResponse response;
        try {
            response = openAiChatModel.call(prompt);
        } catch (Exception e) {
            System.err.println("Spring AI API 호출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return createErrorEntity(senderRole);
        }

        // 4. 응답 파싱
        ChatEntity assistantMessage = null;
        String answer = "오류: 답변을 받아올 수 없습니다.";

        if (response != null && response.getResult() != null) {


            answer = response.getResult().getOutput().getText();

            assistantMessage = ChatEntity.builder()
                    .message(answer)
                    .sender(senderRole)
                    .timestamp(Instant.now().toString())
                    .build();
            System.out.println("ok");
        } else {
            assistantMessage = createErrorEntity(senderRole);
        }

        return assistantMessage;
    }

    // 오류 발생 시 사용할 헬퍼 메소드 (수정 없음)
    private ChatEntity createErrorEntity(String senderRole) {
        return ChatEntity.builder()
                .message("오류: 답변을 받아올 수 없습니다.")
                .sender(senderRole)
                .timestamp(Instant.now().toString())
                .build();
    }
}