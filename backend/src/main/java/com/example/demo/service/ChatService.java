package com.example.demo.service;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    public ChatResponseDTO chatHandler(ChatRequestDTO requestDTO){
        //여기에서 필요에 따라 LLM 응답을 분류해 처리
        return null;
    }
}
