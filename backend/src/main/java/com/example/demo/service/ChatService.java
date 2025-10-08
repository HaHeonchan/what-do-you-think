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

    public ChatResponseDTO askQuestion(ChatRequestDTO requestDTO) {
        List<String> promptKeys = requestDTO.getPromptKeys();

        // 1. 사용자의 메시지를 'user'로 먼저 저장
        ChatEntity userMessage = ChatEntity.builder()
                .message(requestDTO.getQuestion())
                .sender("user")
                .timestamp(Instant.now().toString())
                .build();
        chatRepository.save(userMessage);

        // 2. AI에게 컨텍스트를 제공하기 위해 방금 저장한 메시지를 포함한 전체 대화 기록을 조회
        List<ChatEntity> chatHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
        List<ChatEntity> batchChatHistory = new ArrayList<>(); // 이번 턴의 AI 답변들을 모을 리스트

        System.out.println("\nGPT chatHistory: " + chatHistory);

        if (promptKeys != null && !promptKeys.isEmpty()) {
            for (String promptKey : promptKeys) { // "creator", "critic" 등 프롬프트 키로 순회
                PromptDTO prompt = promptLoader.getPrompts().get(promptKey);
                if (prompt == null) {
                    continue;
                }

                String currentPromptContent = prompt.getContent();
                System.out.println("\nProcessing role: " + promptKey);

                // 3. 수정된 GptService 호출 (promptKey를 sender 역할로 전달)
                // GptService는 이제 AI의 답변을 'creator'와 같은 역할 이름으로 저장합니다.
                ChatEntity answer = gptService.requestGpt(requestDTO, chatHistory, currentPromptContent, promptKey);

                if (answer != null) {
                    batchChatHistory.add(answer);
                }
            }
        }

        // 4. 이번 턴에서 생성된 모든 AI 답변들을 DB에 한번에 저장
        chatRepository.saveAll(batchChatHistory);

        // 5. 생성된 답변들의 메시지만 추출하여 DTO에 담아 반환
        List<String> responses = batchChatHistory.stream()
                .map(ChatEntity::getMessage)
                .toList();

        return new ChatResponseDTO(responses.toString());
    }

    public ChatResponseDTO summarizeConversation() {
        // 1. 전체 대화 기록을 가져옵니다.
        List<ChatEntity> chatHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
        if (chatHistory.isEmpty()) {
            return new ChatResponseDTO("요약할 대화 내용이 없습니다.");
        }

        // 2. 'summarizer' 프롬프트를 로드합니다.
        PromptDTO summarizerPrompt = promptLoader.getPrompts().get("summarizer");
        if (summarizerPrompt == null) {
            return new ChatResponseDTO("오류: Summarizer 프롬프트를 찾을 수 없습니다.");
        }
        String summarizerContent = summarizerPrompt.getContent();

        // 3. GptService를 호출하여 요약을 요청합니다.
        // 이 호출에서는 requestDTO의 특정 내용이 필요하지 않을 수 있습니다.
        ChatRequestDTO dummyRequest = new ChatRequestDTO();
        ChatEntity summary = gptService.requestGpt(dummyRequest, chatHistory, summarizerContent, "summarizer");

        // 4. (선택사항) 생성된 요약 내용을 DB에 저장합니다.
        if (summary != null) {
            chatRepository.save(summary);
        }

        // 5. 요약 결과를 DTO에 담아 반환합니다.
        String summaryContent = (summary != null) ? summary.getMessage() : "요약 생성에 실패했습니다.";
        return new ChatResponseDTO(summaryContent);
    }
}
