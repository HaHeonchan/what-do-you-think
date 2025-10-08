package com.example.demo.service;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.dto.OpenAiApiRequestDTO;
import com.example.demo.dto.OpenAiApiResponseDTO;
import com.example.demo.entity.ChatEntity;
import com.example.demo.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GptService {

    private final RestTemplate restTemplate;
    private final ChatRepository chatRepository;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.model}")
    private String model;

    @Value("${spring.ai.openai.api-base-url}")
    private String API_URL;

    @Value("${spring.ai.openai.max-tokens}")
    private Integer max_tokens;

    public GptService(RestTemplate restTemplate, ChatRepository chatRepository) {
        this.restTemplate = restTemplate;
        this.chatRepository = chatRepository;
    }

    @Transactional //DB를 트렌젝션(묶어서)으로 처리
    public ChatEntity requestGpt(ChatRequestDTO requestDTO, List<ChatEntity> chatHistory, String promptContent, String senderRole) {
        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // requestBody 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_completion_tokens", 4098);

        // 채팅 기록 반영
        List<Map<String, String>> messages = new ArrayList<>(chatHistory.stream()
                .map(chat -> {
                    Map<String, String> message = new HashMap<>();

                    String role = chat.getSender();
                    if (!role.equals("user") && !role.equals("system")) {
                        role = "assistant";
                    }

                    message.put("role", role);
                    message.put("content", chat.getMessage());
                    return message;
                }).toList());

        // 프롬프트 반영
        if (promptContent != null && !promptContent.isEmpty()) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", promptContent);
            messages.add(0, systemMessage); // 보통 시스템 메시지는 맨 앞에 추가합니다.
        }

        // 완성된 messages 리스트를 requestBody에 추가
        requestBody.put("messages", messages);

        // 헤더와 본문을 합쳐 HttpEntity 객체 생성
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        OpenAiApiResponseDTO apiResponse = restTemplate.postForObject(
                API_URL,
                httpEntity,
                OpenAiApiResponseDTO.class
        );

        ChatEntity assistantMessage = null;
        // 응답에서 답변 추출 후 DB에 저장
        String answer = "오류: 답변을 받아올 수 없습니다.";
        if (apiResponse != null && !apiResponse.getChoices().isEmpty()) {
            answer = apiResponse.getChoices().get(0).getMessage().getContent();
            assistantMessage = ChatEntity.builder()
                    .message(answer)
                    .sender(senderRole)
                    .timestamp(Instant.now().toString())
                    .build();
            System.out.println("ok");
        }

        return assistantMessage;
    }

}