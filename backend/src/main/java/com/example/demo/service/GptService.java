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
import java.util.List;
import java.util.UUID;
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

    public GptService(RestTemplate restTemplate, ChatRepository chatRepository) {
        this.restTemplate = restTemplate;
        this.chatRepository = chatRepository;
    }

    @Transactional
    public ChatResponseDTO askQuestion(ChatRequestDTO requestDTO) {
        // 1. 사용자 질문을 DB에 저장
        ChatEntity userMessage = ChatEntity.builder()
                .message(requestDTO.getQuestion())
                .sender(requestDTO.getSender())
                .timestamp(Instant.now().toString())
                .build();
        chatRepository.save(userMessage);

        // 2. 이전 대화 기록을 시간순으로 불러오기
        List<ChatEntity> chatHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));

        System.out.println("userMessage saved: " + chatHistory);

        // 3. OpenAI API 요청에 맞게 메시지 목록 생성
        List<OpenAiApiRequestDTO.Message> messages = chatHistory.stream()
                .map(chat -> new OpenAiApiRequestDTO.Message(chat.getSender(), chat.getMessage()))
                .collect(Collectors.toList());

        // 4. HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 5. 요청 본문(Body) 생성 (전체 대화 기록 포함)
        OpenAiApiRequestDTO apiRequest = new OpenAiApiRequestDTO(model, messages);

        // 6. 헤더와 본문을 합쳐 HttpEntity 객체 생성
        HttpEntity<OpenAiApiRequestDTO> httpEntity = new HttpEntity<>(apiRequest, headers);
        
        // 7. RestTemplate을 사용하여 POST 요청 보내기
        OpenAiApiResponseDTO apiResponse = restTemplate.postForObject(
                API_URL,
                httpEntity,
                OpenAiApiResponseDTO.class
        );

        // 8. 응답에서 답변 추출 후 DB에 저장
        String answer = "오류: 답변을 받아올 수 없습니다.";
        if (apiResponse != null && !apiResponse.getChoices().isEmpty()) {
            answer = apiResponse.getChoices().get(0).getMessage().getContent();
            ChatEntity assistantMessage = ChatEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .message(answer)
                    .sender(requestDTO.getReceiver())
                    .timestamp(Instant.now().toString())
                    .build();
            chatRepository.save(assistantMessage);
        }

        return new ChatResponseDTO(answer);
    }

}