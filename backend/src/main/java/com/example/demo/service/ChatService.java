package com.example.demo.service;

import com.example.demo.config.PromptLoader;
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
    private final PromptLoader promptLoader;

    public ChatService(ChatRepository chatRepository, GptService gptService, PromptLoader promptLoader) {
        this.chatRepository = chatRepository;
        this.gptService = gptService;
        this.promptLoader = promptLoader;
    }

    public ChatResponseDTO askQuestion(ChatRequestDTO requestDTO) {
        // 1) 사용자 입력 저장
        ChatEntity userMessage = ChatEntity.builder()
                .message(requestDTO.getQuestion())
                .sender("user")
                .timestamp(Instant.now().toString())
                .build();
        chatRepository.save(userMessage);

        // 2) 컨텍스트 로드 및 첫 턴 판정
        List<ChatEntity> allHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
        boolean isFirstTurn = isFirstTurn(allHistory);

        // 라운드 수 및 프롬프트 키 검증
        int rounds = requestDTO.getConversationRounds() != null ? requestDTO.getConversationRounds() : 1;
        List<String> promptKeys = requestDTO.getPromptKeys();
        if (promptKeys == null || promptKeys.isEmpty()) {
            return new ChatResponseDTO("프롬프트 키가 없습니다.");
        }

        for (int round = 0; round < rounds; round++) {
            List<ChatEntity> roundAnswers = new ArrayList<>();

            for (String roleKey : promptKeys) {
                String systemContent = buildSystemContent(roleKey, isFirstTurn);
                List<java.util.Map<String, String>> messages = buildMessages(systemContent, requestDTO.getQuestion(), allHistory, roleKey, isFirstTurn);

                ChatEntity answer = gptService.requestGpt(messages, roleKey);
                if (answer != null) {
                    roundAnswers.add(answer);
                }
            }

            if (!roundAnswers.isEmpty()) {
                chatRepository.saveAll(roundAnswers);
                // 메모리 컨텍스트에도 즉시 반영 (불필요한 재조회 방지)
                allHistory.addAll(roundAnswers);
                isFirstTurn = false;
            }
        }

        String summary = summarize(allHistory);
        return new ChatResponseDTO(summary);
    }

    private boolean isFirstTurn(List<ChatEntity> history) {
        return history.stream().noneMatch(c -> !"user".equals(c.getSender()) && !"system".equals(c.getSender()));
    }

    private String buildSystemContent(String roleKey, boolean isFirstTurn) {
        String base = promptLoader.getPrompt(roleKey);
        String guide = promptLoader.getInstruction(isFirstTurn ? "initial_response" : "debate_response");
        String safeBase = base == null ? "" : base;
        String safeGuide = guide == null ? "" : guide;
        return safeBase + (safeGuide.isEmpty() ? "" : "\n\n" + safeGuide);
    }

    private List<java.util.Map<String, String>> buildMessages(String systemContent, String userQuestion, List<ChatEntity> history, String currentRole, boolean isFirstTurn) {
        List<java.util.Map<String, String>> messages = new ArrayList<>();

        java.util.Map<String, String> system = new java.util.HashMap<>();
        system.put("role", "system");
        system.put("content", systemContent);
        messages.add(system);

        java.util.Map<String, String> user = new java.util.HashMap<>();
        user.put("role", "user");
        user.put("content", userQuestion);
        messages.add(user);

        if (!isFirstTurn) {
            // 최근 사용자 이후의 다른 에이전트 응답들만 포함
            int lastUserIdx = findLastUserIndex(history);
            for (int i = lastUserIdx + 1; i < history.size(); i++) {
                ChatEntity chat = history.get(i);
                String sender = chat.getSender();
                if (!"user".equals(sender) && !"system".equals(sender) && !currentRole.equals(sender)) {
                    java.util.Map<String, String> assistant = new java.util.HashMap<>();
                    assistant.put("role", "assistant");
                    assistant.put("content", chat.getMessage());
                    messages.add(assistant);
                }
            }
        }

        return messages;
    }

    private int findLastUserIndex(List<ChatEntity> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).getSender())) {
                return i;
            }
        }
        return -1;
    }

    private String summarize(List<ChatEntity> history) {
        if (history.isEmpty()) return "요약할 대화 내용이 없습니다.";

        String summarizer = promptLoader.getPrompt("summarizer");
        if (summarizer == null) return "요약 프롬프트를 찾을 수 없습니다.";

        List<java.util.Map<String, String>> messages = new ArrayList<>();
        java.util.Map<String, String> system = new java.util.HashMap<>();
        system.put("role", "system");
        system.put("content", summarizer);
        messages.add(system);

        for (ChatEntity chat : history) {
            java.util.Map<String, String> m = new java.util.HashMap<>();
            String role = chat.getSender();
            if (!"user".equals(role) && !"system".equals(role)) role = "assistant";
            m.put("role", role);
            m.put("content", chat.getMessage());
            messages.add(m);
        }

        ChatEntity summary = gptService.requestGpt(messages, "summarizer");
        if (summary != null) {
            chatRepository.save(summary);
            return summary.getMessage();
        }
        return "요약 생성에 실패했습니다.";
    }

    public ChatResponseDTO summarizeConversation() {
        // 1. 전체 대화 기록을 가져옵니다.
        List<ChatEntity> chatHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
        if (chatHistory.isEmpty()) {
            return new ChatResponseDTO("요약할 대화 내용이 없습니다.");
        }

        // 2. 'summarizer' 프롬프트를 로드합니다.
        String summarizerContent = promptLoader.getPrompt("summarizer");
        if (summarizerContent == null) {
            return new ChatResponseDTO("오류: Summarizer 프롬프트를 찾을 수 없습니다.");
        }

        // 3. GptService를 호출하여 요약을 요청합니다.
        // system + history 로 메시지 구성
        List<java.util.Map<String, String>> messages = new java.util.ArrayList<>();
        java.util.Map<String, String> systemMessage = new java.util.HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", summarizerContent);
        messages.add(systemMessage);

        for (ChatEntity chat : chatHistory) {
            java.util.Map<String, String> msg = new java.util.HashMap<>();
            String role = chat.getSender();
            if (!role.equals("user") && !role.equals("system")) {
                role = "assistant";
            }
            msg.put("role", role);
            msg.put("content", chat.getMessage());
            messages.add(msg);
        }

        ChatEntity summary = gptService.requestGpt(messages, "summarizer");

        // 4. (선택사항) 생성된 요약 내용을 DB에 저장합니다.
        if (summary != null) {
            chatRepository.save(summary);
        }

        // 5. 요약 결과를 DTO에 담아 반환합니다.
        String summaryContent = (summary != null) ? summary.getMessage() : "요약 생성에 실패했습니다.";
        return new ChatResponseDTO(summaryContent);
    }
}