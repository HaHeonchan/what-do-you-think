package com.example.demo.service;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.dto.OpenAiApiRequestDTO;
import com.example.demo.dto.OpenAiApiResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GptService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.model}")
    private String model;

    @Value("${spring.ai.openai.api-base-url}")
    private String API_URL;

    public GptService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ChatResponseDTO askQuestion(ChatRequestDTO requestDTO) {
        // 1. HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 2. 요청 본문(Body) 생성
        OpenAiApiRequestDTO apiRequest = new OpenAiApiRequestDTO(model, requestDTO.getQuestion());

        // 3. 헤더와 본문을 합쳐 HttpEntity 객체 생성
        HttpEntity<OpenAiApiRequestDTO> httpEntity = new HttpEntity<>(apiRequest, headers);

        // 4. RestTemplate을 사용하여 POST 요청 보내기
        OpenAiApiResponseDTO apiResponse = restTemplate.postForObject(
                API_URL,
                httpEntity,
                OpenAiApiResponseDTO.class
        );

        // 5. 응답에서 답변 추출
        String answer = "오류: 답변을 받아올 수 없습니다.";
        if (apiResponse != null && !apiResponse.getChoices().isEmpty()) {
            answer = apiResponse.getChoices().get(0).getMessage().getContent();
        }

        return new ChatResponseDTO(answer);
    }
}