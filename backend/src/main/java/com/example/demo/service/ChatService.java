package com.example.demo.service;

import com.example.demo.config.PromptLoader;
import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.dto.ModeratorResponseDTO;
import com.example.demo.entity.ChatEntity;
import com.example.demo.repository.ChatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final GptService gptService;
    private final PromptLoader promptLoader;
    private final Executor gptExecutor;
    private final ObjectMapper objectMapper;

    public ChatService(ChatRepository chatRepository, GptService gptService, PromptLoader promptLoader, 
                      Executor gptExecutor, ObjectMapper objectMapper) {
        this.chatRepository = chatRepository;
        this.gptService = gptService;
        this.promptLoader = promptLoader;
        this.gptExecutor = gptExecutor;
        this.objectMapper = objectMapper;
    }

    public ChatResponseDTO askQuestion(ChatRequestDTO requestDTO) {
        // 사용자 입력 저장
        ChatEntity userMessage = ChatEntity.builder()
                .message(requestDTO.getQuestion())
                .sender("user")
                .timestamp(Instant.now().toString())
                .build();
        chatRepository.save(userMessage);

        // 프롬프트 키 검증
        List<String> promptKeys = requestDTO.getPromptKeys();
        if (promptKeys == null || promptKeys.isEmpty()) {
            return new ChatResponseDTO("프롬프트 키가 없습니다.");
        }

        // 대화 히스토리 로드
        List<ChatEntity> allHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
        
        // 대화 라운드 수 설정
        int rounds = 1;
        if(requestDTO.getConversationRounds() != null) {
            rounds = requestDTO.getConversationRounds();
        }

        // 라운드별 처리
        for (int round = 0; round < rounds; round++) {
            System.out.println("\n========== 라운드 " + (round + 1) + " ==========");
            
            // 사회자에게 누구에게 물어볼지 결정 요청
            List<Map<String, String>> moderatorMessages = buildMessages("moderator", requestDTO.getQuestion(), allHistory);
            ChatEntity moderatorResponse = gptService.requestGpt(moderatorMessages, "moderator");
            
            // 사회자 응답 출력
            if (moderatorResponse != null) {
                System.out.println("[사회자 원본 응답]");
                System.out.println(moderatorResponse.getMessage());
                System.out.println("---");
            } else {
                System.out.println("[사회자 응답 없음]");
            }
            
            // 사회자 응답 파싱 (request 배열 지원)
            List<CompletableFuture<ChatEntity>> futures = new ArrayList<>();
            if (moderatorResponse != null) {
                ModeratorResponseDTO decision = parseModerator(moderatorResponse.getMessage());
                if (decision != null && decision.getRequest() != null && !decision.getRequest().isEmpty()) {
                    System.out.println("[파싱 성공] 사회자 요청 수: " + decision.getRequest().size());
                    decision.getRequest().stream()
                            .filter(item -> {
                                boolean allowed = promptKeys != null && promptKeys.contains(item.getRoleKey());
                                if (!allowed) {
                                    System.out.println("[필터링] 허용되지 않은 roleKey 제거: " + item.getRoleKey());
                                }
                                return allowed;
                            })
                            .forEach(item -> {
                        String roleKey = item.getRoleKey();
                        String questionToExperts = item.getMessages() != null ? item.getMessages() : requestDTO.getQuestion();
                        List<Map<String, String>> messages = buildMessages(roleKey, questionToExperts, allHistory);
                        CompletableFuture<ChatEntity> future = CompletableFuture
                                .supplyAsync(() -> gptService.requestGpt(messages, roleKey), gptExecutor)
                                .orTimeout(45, TimeUnit.SECONDS)
                                .exceptionally(ex -> null);
                        futures.add(future);
                    });
                } else {
                    System.out.println("[파싱 실패 - 기본 프롬프트 키 사용]");
                }
            }

            // 사회자 요청이 없거나 파싱 실패 시 기본 동작
            if (futures.isEmpty()) {
                for (String roleKey : promptKeys) {
                    List<Map<String, String>> messages = buildMessages(roleKey, requestDTO.getQuestion(), allHistory);
                    CompletableFuture<ChatEntity> future = CompletableFuture
                            .supplyAsync(() -> gptService.requestGpt(messages, roleKey), gptExecutor)
                            .orTimeout(45, TimeUnit.SECONDS)
                            .exceptionally(ex -> null);
                    futures.add(future);
                }
            }

            System.out.println("================================\n");

            List<ChatEntity> roundAnswers = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(answer -> answer != null)
                    .toList();

            if (!roundAnswers.isEmpty()) {
                chatRepository.saveAll(roundAnswers);
                allHistory.addAll(roundAnswers);
            }
        }

        return new ChatResponseDTO(summarize(allHistory));
    }

    private List<Map<String, String>> buildMessages(String roleKey, String userQuestion, List<ChatEntity> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System 메시지
        String systemContent = promptLoader.getPrompt(roleKey);
        
        // 사회자는 debate_response 지시사항을 붙이지 않음 (JSON 형식 유지를 위해)
        if (systemContent != null) {
            Map<String, String> system = new HashMap<>();
            system.put("role", "system");
            
            if ("moderator".equals(roleKey)) {
                // 사회자는 프롬프트만 사용
                system.put("content", systemContent);
            } else {
                // 다른 역할들은 debate_response 지시사항 추가
                String instruction = promptLoader.getInstruction("debate_response");
                system.put("content", instruction != null ? systemContent + "\n\n" + instruction : systemContent);
            }
            messages.add(system);
        }

        // User 메시지
        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userQuestion);
        messages.add(user);

        // 최근 다른 에이전트의 응답들 추가
        int lastUserIdx = findLastUserIndex(history);
        if (lastUserIdx >= 0) {
            for (int i = lastUserIdx + 1; i < history.size(); i++) {
                ChatEntity chat = history.get(i);
                if (!chat.getSender().equals(roleKey) && !chat.getSender().equals("user")) {
                    Map<String, String> assistant = new HashMap<>();
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
            if (history.get(i).getSender().equals("user")) {
                return i;
            }
        }
        return -1;
    }

    private String summarize(List<ChatEntity> history) {
        if (history.isEmpty()) return "요약할 대화 내용이 없습니다.";

        String summarizerPrompt = promptLoader.getPrompt("summarizer");
        if (summarizerPrompt == null) return "요약 프롬프트를 찾을 수 없습니다.";

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", summarizerPrompt);
        messages.add(system);

        for (ChatEntity chat : history) {
            Map<String, String> msg = new HashMap<>();
            String role = chat.getSender().equals("user") ? "user" : "assistant";
            msg.put("role", role);
            msg.put("content", chat.getMessage());
            messages.add(msg);
        }

        ChatEntity summary = gptService.requestGpt(messages, "summarizer");
        if (summary != null) {
            chatRepository.save(summary);
            return summary.getMessage();
        }
        return "요약 생성에 실패했습니다.";
    }

    public ChatResponseDTO summarizeConversation() {
        List<ChatEntity> chatHistory = chatRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
        return new ChatResponseDTO(summarize(chatHistory));
    }

    // 사회자 응답 JSON 파싱
    private ModeratorResponseDTO parseModerator(String responseText) {
        try {
            // GPT가 ```json ... ``` 형태로 반환할 경우 추출
            String jsonText = responseText.trim();
            if (jsonText.contains("```json")) {
                jsonText = jsonText.substring(jsonText.indexOf("```json") + 7, jsonText.lastIndexOf("```")).trim();
            } else if (jsonText.contains("```")) {
                jsonText = jsonText.substring(jsonText.indexOf("```") + 3, jsonText.lastIndexOf("```")).trim();
            }
            
            return objectMapper.readValue(jsonText, ModeratorResponseDTO.class);
        } catch (Exception e) {
            System.err.println("사회자 응답 파싱 실패: " + e.getMessage());
            return null;
        }
    }
}