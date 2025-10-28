package com.example.demo.service;

import com.example.demo.dto.OpenAiApiResponseDTO;
import com.example.demo.entity.ChatEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GptService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.model}")
    private String model;

    @Value("${spring.ai.openai.api-base-url}")
    private String API_URL;

    @Value("${spring.ai.openai.max-tokens}")
    private Integer max_tokens;

    public GptService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Transactional //DB를 트렌젝션(묶어서)으로 처리
    public ChatEntity requestGpt(List<Map<String, String>> messages, String senderRole) {
        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // requestBody 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_completion_tokens", 4098);

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