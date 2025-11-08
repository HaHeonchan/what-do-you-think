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
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GptService {
    private final OpenAiChatModel openAiChatModel;

    public GptService(OpenAiChatModel openAiChatModel) {
        this.openAiChatModel = openAiChatModel;
    }


    @Transactional
    public ChatEntity requestGpt(List<Map<String, String>> messages, String senderRole) {

        // 1. 공통 옵션 빌더 생성
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model("gpt-5-nano-2025-08-07")
                .temperature(1.0);
        // 2. [핵심] "moderator"일 경우 JSON 모드 활성화
        if ("moderator".equals(senderRole)) {
            String schema = """
            {
              "type": "object",
              "properties": {
                "request": {
                  "type": "array",
                  "minItems": 1,
                  "items": {
                    "type": "object",
                    "properties": {
                      "roleKey": { "type": "string" },
                      "messages": { "type": "string", "maxLength": 200 }
                    },
                    "required": ["roleKey", "messages"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["request"],
              "additionalProperties": false
            }
           """;
            optionsBuilder.responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, schema));
        }

        // 3. 최종 옵션 빌드
        OpenAiChatOptions options = optionsBuilder.build();

        // 4. ChatService의 List<Map>을 Spring AI의 List<Message>로 변환
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
                            return new UserMessage(content); // 기본값
                    }
                })
                .collect(Collectors.toList());

        Prompt prompt = new Prompt(springAiMessages, options);

        ChatResponse response;
        try {
            // .call()이 Prompt에 포함된 options를 사용합니다.
            response = openAiChatModel.call(prompt);
        } catch (Exception e) {
            System.err.println("Spring AI API 호출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return createErrorEntity(senderRole);
        }

        // 6. 응답 파싱 및 토큰 사용량 추출
        ChatEntity assistantMessage = null;
        String answer = "오류: 답변을 받아올 수 없습니다.";
        Long tokensUsed = 0L;

        if (response != null && response.getResult() != null) {
            answer = response.getResult().getOutput().getText();
            
            // 토큰 사용량 추출
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                var usage = response.getMetadata().getUsage();
                tokensUsed = usage.getTotalTokens() != null ? usage.getTotalTokens() : 0L;
            }

            assistantMessage = ChatEntity.builder()
                    .message(answer)
                    .sender(senderRole)
                    .timestamp(Instant.now().toString())
                    .tokensUsed(tokensUsed)
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