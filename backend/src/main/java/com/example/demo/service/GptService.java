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
    public ChatEntity requestGpt(ChatRequestDTO requestDTO, List<ChatEntity> chatHistory, String promptContent) {
        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // requestBody 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 126);
        requestBody.put("temperature", 0.3);

        // 채팅 기록 반영
        List<Map<String, String>> messages = chatHistory.stream()
                .map(chat -> {
                    Map<String, String> message = new HashMap<>();
                    message.put("role", chat.getSender()); // sender를 role로 매핑
                    message.put("content", chat.getMessage()); // message를 content로 매핑
                    return message;
                }).toList();

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

        // 응답 처리
        ChatEntity assistantMessage = null;
        if (apiResponse != null && !apiResponse.getChoices().isEmpty()) {
            String answer = apiResponse.getChoices().get(0).getMessage().getContent();
            assistantMessage = ChatEntity.builder()
                    .message(answer)
                    .sender("user")
                    .timestamp(Instant.now().toString())
                    .build();
            System.out.println("ok");
        } else {
            // 오류 처리 로직
            assistantMessage = ChatEntity.builder()
                    .message("오류: 답변을 받아올 수 없습니다.")
                    .sender("system")
                    .timestamp(Instant.now().toString())
                    .build();
        }

        return assistantMessage;

        //        List<ChatEntity> aiResponses = new ArrayList<>();
//
//        // 3. OpenAI API 요청에 맞게 메시지 목록 생성
//        List<OpenAiApiRequestDTO.Message> messages = chatHistory.stream()
//                .map(chat -> new OpenAiApiRequestDTO.Message(chat.getSender(), chat.getMessage()))
//                .collect(Collectors.toList());
//
//        if (promptContent != null && !promptContent.isEmpty()) {
//            messages.add(new OpenAiApiRequestDTO.Message("system", promptContent));
//        }
//
//        // 4. HTTP 헤더 설정
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(apiKey);
//
//        // 5. 요청 본문(Body) 생성 (전체 대화 기록 포함)
//        OpenAiApiRequestDTO apiRequest = new OpenAiApiRequestDTO(model, messages, max_tokens);
//
//        // 6. 헤더와 본문을 합쳐 HttpEntity 객체 생성
//        HttpEntity<OpenAiApiRequestDTO> httpEntity = new HttpEntity<>(apiRequest, headers);
//
//        // 7. RestTemplate을 사용하여 POST 요청 보내기
//        OpenAiApiResponseDTO apiResponse = restTemplate.postForObject(
//                API_URL,
//                httpEntity,
//                OpenAiApiResponseDTO.class
//        );
//        ChatEntity assistantMessage = null;
//        // 8. 응답에서 답변 추출 후 DB에 저장
//        String answer = "오류: 답변을 받아올 수 없습니다.";
//        if (apiResponse != null && !apiResponse.getChoices().isEmpty()) {
//            answer = apiResponse.getChoices().get(0).getMessage().getContent();
//            assistantMessage = ChatEntity.builder()
//                    .message(answer)
//                    .sender("user") //사실은 LLM
//                    .timestamp(Instant.now().toString())
//                    .build();
//            System.out.println("ok");
//        }
//
//        return assistantMessage; // List<ChatEntity>를 반환합니다.
    }

}