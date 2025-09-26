package com.example.demo.service;

import com.example.demo.service.GptService;
import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.entity.ChatEntity;
import com.example.demo.repository.ChatRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final GptService gptService;

    public ChatService(ChatRepository chatRepository, GptService gptService) {
        this.chatRepository = chatRepository;
        this.gptService = gptService;
    }

    public ChatResponseDTO askQuestion(ChatRequestDTO requestDTO){
        //사용자의 응답을 받아 그 응답에 대해 스스로 Q&A 하는 기능

        ChatEntity userMessage = ChatEntity.builder()
                .message(requestDTO.getQuestion())
                .sender(requestDTO.getSender())
                .timestamp(Instant.now().toString())
                .build();
        chatRepository.save(userMessage);

        for(int i=0; i<3; i++) { //batch
            List<ChatEntity> chatHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
            List<ChatEntity> batchChatHistory = new ArrayList<>();
            System.out.println("userMessage saved: " + chatHistory);

            for(int j=0; j<1; j++){
                ChatEntity answer = gptService.requestGpt(requestDTO, chatHistory);
                if(answer != null){
                    batchChatHistory.add(answer);
                }
            }

            chatRepository.saveAll(batchChatHistory);
        }

        return new ChatResponseDTO(null);
    }

}
