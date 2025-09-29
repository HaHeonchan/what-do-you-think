package com.example.demo.service;

import com.example.demo.config.PromptLoader;
import com.example.demo.dto.PromptDTO;
import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.entity.ChatEntity;
import com.example.demo.repository.ChatRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final GptService gptService;
    private final PromptLoader promptLoader;

    public ChatService(ChatRepository chatRepository, GptService gptService, PromptLoader promptLoader) {
        this.chatRepository = chatRepository;
        this.gptService = gptService;
        this.promptLoader = promptLoader;
    }

    public ChatResponseDTO askQuestion(ChatRequestDTO requestDTO){
        //사용자의 응답을 받아 그 응답에 대해 스스로 Q&A 하는 기능
        List<String> promptKeys = requestDTO.getPromptKeys();

        // 2. String 리스트가 아닌 Prompt 객체 리스트를 받도록 변경
        List<PromptDTO> selectedPrompts = new ArrayList<>();

        if (promptKeys != null && !promptKeys.isEmpty()) {
            selectedPrompts = promptKeys.stream()
                    // promptLoader에서 key로 Prompt 객체를 조회
                    .map(key -> promptLoader.getPrompts().get(key))
                    .filter(Objects::nonNull)
                    .toList();
        }

        System.out.println("조회된 실제 프롬프트 객체: " + selectedPrompts);

        List<String> promptContents = selectedPrompts.stream()
                .map(PromptDTO::getContent)
                .toList();
        System.out.println("프롬프트 내용만 추출: " + promptContents);

        ChatEntity userMessage = ChatEntity.builder()
                .message(requestDTO.getQuestion())
                .sender(requestDTO.getSender())
                .timestamp(Instant.now().toString())
                .build();
        chatRepository.save(userMessage);


        for(int i=0; i<1; i++) {
            List<ChatEntity> chatHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
            List<ChatEntity> batchChatHistory = new ArrayList<>(); // 이번 턴의 답변들을 모을 리스트

            System.out.println("GPT 요청 직전 대화 기록: " + chatHistory);

            for (PromptDTO prompt : selectedPrompts) { // 리스트의 각 PromptDTO를 'prompt' 변수에 담습니다.

                // 4. 현재 순서의 prompt 객체에서 content 문자열만 추출합니다.
                String currentPromptContent = prompt.getContent();

                System.out.println("현재 적용되는 프롬프트 역할: " + prompt.getRole()); // 역할(role)도 확인 가능

                // 5. gptService에 추출한 프롬프트 내용(currentPromptContent)을 전달합니다.
                ChatEntity answer = gptService.requestGpt(requestDTO, chatHistory, currentPromptContent);
                if(answer != null){
                    batchChatHistory.add(answer);
                }
            }

            // 이번 턴에서 생성된 모든 답변(batchChatHistory)을 DB에 한번에 저장합니다.
            chatRepository.saveAll(batchChatHistory);
        }

        return new ChatResponseDTO(null);
    }
}
