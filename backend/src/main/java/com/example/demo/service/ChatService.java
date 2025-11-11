package com.example.demo.service;

import com.example.demo.config.PromptLoader;
import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.dto.ModeratorResponseDTO;
import com.example.demo.entity.ChatEntity;
import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.Member;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final GptService gptService;
    private final PromptLoader promptLoader;
    private final Executor gptExecutor;
    private final ObjectMapper objectMapper;
    private final GoogleCustomSearchService googleCustomSearchService;

    public ChatService(ChatRepository chatRepository, ChatRoomRepository chatRoomRepository,
                      MemberRepository memberRepository, GptService gptService, PromptLoader promptLoader, 
                      Executor gptExecutor, ObjectMapper objectMapper, GoogleCustomSearchService googleCustomSearchService) {
        this.chatRepository = chatRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.memberRepository = memberRepository;
        this.gptService = gptService;
        this.promptLoader = promptLoader;
        this.gptExecutor = gptExecutor;
        this.objectMapper = objectMapper;
        this.googleCustomSearchService = googleCustomSearchService;
    }

    @Transactional
    public ChatResponseDTO askQuestion(ChatRequestDTO requestDTO) {
        // 사용자 검증
        if (requestDTO.getUserId() == null) {
            return new ChatResponseDTO("사용자 ID가 필요합니다.");
        }
        
        Member member = memberRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 대화방 가져오기 또는 생성
        final ChatRoom chatRoom;
        if (requestDTO.getChatRoomId() != null) {
            chatRoom = chatRoomRepository.findByIdAndMember(requestDTO.getChatRoomId(), member)
                    .orElseThrow(() -> new RuntimeException("대화방을 찾을 수 없거나 접근 권한이 없습니다."));
            
            // 이미 처리 중인지 확인
            if (Boolean.TRUE.equals(chatRoom.getIsProcessing())) {
                throw new RuntimeException("이미 처리 중인 요청이 있습니다. 잠시 후 다시 시도해주세요.");
            }
        } else {
            // 새 대화방 생성
            String title = requestDTO.getQuestion().length() > 50 
                    ? requestDTO.getQuestion().substring(0, 50) + "..." 
                    : requestDTO.getQuestion();
            ChatRoom newChatRoom = ChatRoom.builder()
                    .member(member)
                    .title(title)
                    .isProcessing(false)
                    .build();
            chatRoom = chatRoomRepository.save(newChatRoom);
        }

        // 처리 시작: isProcessing을 true로 설정
        chatRoom.setIsProcessing(true);
        chatRoomRepository.save(chatRoom);

        try {
            // 사용자 입력 저장
        ChatEntity userMessage = ChatEntity.builder()
                .chatRoom(chatRoom)
                .message(requestDTO.getQuestion())
                .sender("user")
                .timestamp(Instant.now().toString())
                .build();
        chatRepository.save(userMessage);

        // 프롬프트 키 검증 및 기본값 설정
        List<String> promptKeys = requestDTO.getPromptKeys();
        if (promptKeys == null || promptKeys.isEmpty()) {
            // 기본 역할: creator, critic, analyst
            promptKeys = List.of("creator", "critic", "analyst");
        }
        final List<String> finalPromptKeys = promptKeys;

        // 해당 대화방의 대화 히스토리 로드
        List<ChatEntity> allHistory = chatRepository.findByChatRoomOrderByTimestampAsc(chatRoom);
        
        // 최대 대화 횟수 설정 (사용자가 지정한 횟수만큼만 반복)
        int maxRounds = 10; // 기본값
        if(requestDTO.getConversationRounds() != null && requestDTO.getConversationRounds() > 0) {
            maxRounds = requestDTO.getConversationRounds();
        }

        // 자율적인 대화 진행 (모더레이터가 종료할 때까지 또는 최대 횟수까지)
        int round = 0;
        boolean shouldEnd = false;
        
        while (round < maxRounds && !shouldEnd) {
            round++;
            System.out.println("\n========== 대화 " + round + "회차 (최대 " + maxRounds + "회차) ==========");
            
            // 사회자에게 누구에게 물어볼지 결정 요청
            List<Map<String, String>> moderatorMessages = buildMessages("moderator", requestDTO.getQuestion(), allHistory, chatRoom.getNote(), round, maxRounds);
            ChatEntity moderatorResponse = gptService.requestGpt(moderatorMessages, "moderator");
            
            // 사회자 응답 출력 및 저장
            if (moderatorResponse != null) {
                moderatorResponse.setChatRoom(chatRoom);
                chatRepository.save(moderatorResponse);
                allHistory.add(moderatorResponse);
                System.out.println("[사회자 원본 응답]");
                System.out.println(moderatorResponse.getMessage());
                System.out.println("---");
                
                // 사회자 응답 파싱 및 종료 여부 확인
                ModeratorResponseDTO decision = parseModerator(moderatorResponse.getMessage());
                if (decision != null) {
                    // 종료 여부 확인
                    if (Boolean.TRUE.equals(decision.getShouldEnd())) {
                        System.out.println("[사회자 결정] 대화를 종료합니다.");
                        shouldEnd = true;
                        // 종료 시에도 통계 반영
                        if (moderatorResponse.getTokensUsed() != null && moderatorResponse.getTokensUsed() > 0) {
                            chatRoom.addTokensUsed(moderatorResponse.getTokensUsed());
                        }
                        chatRoom.incrementRoleParticipation("moderator");
                        break; // 루프 종료
                    }
                    
                    // 요청이 있으면 처리
                    if (decision.getRequest() != null && !decision.getRequest().isEmpty()) {
                        System.out.println("[파싱 성공] 사회자 요청 수: " + decision.getRequest().size());
                        
                        // 사회자 요청 처리
                        List<CompletableFuture<ChatEntity>> futures = new ArrayList<>();
                        decision.getRequest().stream()
                                .filter(item -> {
                                    boolean allowed = finalPromptKeys != null && finalPromptKeys.contains(item.getRoleKey());
                                    if (!allowed) {
                                        System.out.println("[필터링] 허용되지 않은 roleKey 제거: " + item.getRoleKey());
                                    }
                                    return allowed;
                                })
                                .forEach(item -> {
                            String roleKey = item.getRoleKey();
                            String questionToExperts = item.getMessages() != null ? item.getMessages() : requestDTO.getQuestion();
                            List<Map<String, String>> messages = buildMessages(roleKey, questionToExperts, allHistory, null);
                            CompletableFuture<ChatEntity> future = CompletableFuture
                                    .supplyAsync(() -> gptService.requestGpt(messages, roleKey), gptExecutor)
                                    .orTimeout(45, TimeUnit.SECONDS)
                                    .exceptionally(ex -> null);
                            futures.add(future);
                        });
                        
                        // 전문가 응답 대기 및 저장
                        List<ChatEntity> roundAnswers = futures.stream()
                                .map(CompletableFuture::join)
                                .filter(answer -> answer != null)
                                .peek(answer -> {
                                    answer.setChatRoom(chatRoom);
                                    // 통계 업데이트: 역할별 참여 횟수 및 토큰 사용량
                                    if (answer.getSender() != null && !answer.getSender().equals("user")) {
                                        chatRoom.incrementRoleParticipation(answer.getSender());
                                    }
                                    if (answer.getTokensUsed() != null && answer.getTokensUsed() > 0) {
                                        chatRoom.addTokensUsed(answer.getTokensUsed());
                                    }
                                })
                                .toList();

                        if (!roundAnswers.isEmpty()) {
                            chatRepository.saveAll(roundAnswers);
                            allHistory.addAll(roundAnswers);
                        }
                    } else {
                        System.out.println("[사회자 요청 없음] 다음 라운드로 진행합니다.");
                    }
                } else {
                    System.out.println("[파싱 실패] 기본 동작으로 진행합니다.");
                    // 파싱 실패 시 기본 동작
                    List<CompletableFuture<ChatEntity>> futures = new ArrayList<>();
                    for (String roleKey : finalPromptKeys) {
                        List<Map<String, String>> messages = buildMessages(roleKey, requestDTO.getQuestion(), allHistory, null);
                        CompletableFuture<ChatEntity> future = CompletableFuture
                                .supplyAsync(() -> gptService.requestGpt(messages, roleKey), gptExecutor)
                                .orTimeout(45, TimeUnit.SECONDS)
                                .exceptionally(ex -> null);
                        futures.add(future);
                    }
                    
                    List<ChatEntity> roundAnswers = futures.stream()
                            .map(CompletableFuture::join)
                            .filter(answer -> answer != null)
                            .peek(answer -> {
                                answer.setChatRoom(chatRoom);
                                if (answer.getSender() != null && !answer.getSender().equals("user")) {
                                    chatRoom.incrementRoleParticipation(answer.getSender());
                                }
                                if (answer.getTokensUsed() != null && answer.getTokensUsed() > 0) {
                                    chatRoom.addTokensUsed(answer.getTokensUsed());
                                }
                            })
                            .toList();

                    if (!roundAnswers.isEmpty()) {
                        chatRepository.saveAll(roundAnswers);
                        allHistory.addAll(roundAnswers);
                    }
                }
                
                // 사회자 응답 통계 반영
                if (moderatorResponse.getTokensUsed() != null && moderatorResponse.getTokensUsed() > 0) {
                    chatRoom.addTokensUsed(moderatorResponse.getTokensUsed());
                }
                chatRoom.incrementRoleParticipation("moderator");
            } else {
                System.out.println("[사회자 응답 없음] 대화를 종료합니다.");
                shouldEnd = true;
            }
            
            System.out.println("================================\n");
        }
        
        // 최대 횟수에 도달한 경우 알림
        if (round >= maxRounds && !shouldEnd) {
            System.out.println("[최대 횟수 도달] " + maxRounds + "회차에 도달하여 대화를 종료합니다.");
        }

        String summaryText = summarize(allHistory, chatRoom, chatRoom.getNote());
        // 노트 업데이트 (요약 저장)
        chatRoom.setNote(summaryText);
        // 처리 완료: isProcessing을 false로 설정
        chatRoom.setIsProcessing(false);
        // 대화방 업데이트 시간 갱신
        chatRoomRepository.save(chatRoom);
        return new ChatResponseDTO(summaryText);
        } catch (Exception e) {
            // 에러 발생 시에도 처리 상태 해제
            chatRoom.setIsProcessing(false);
            chatRoomRepository.save(chatRoom);
            throw e;
        }
    }

    private List<Map<String, String>> buildMessages(String roleKey, String userQuestion, List<ChatEntity> history, String existingNote) {
        return buildMessages(roleKey, userQuestion, history, existingNote, 0, 0);
    }
    
    private List<Map<String, String>> buildMessages(String roleKey, String userQuestion, List<ChatEntity> history, String existingNote, int currentRound, int maxRounds) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System 메시지
        String systemContent = promptLoader.getPrompt(roleKey);
        
        // 사회자는 debate_response 지시사항을 붙이지 않음 (JSON 형식 유지를 위해)
        if (systemContent != null) {
            Map<String, String> system = new HashMap<>();
            system.put("role", "system");
            
            // 기존 노트가 있으면 참고하도록 추가
            if (existingNote != null && !existingNote.trim().isEmpty() && "summarizer".equals(roleKey)) {
                systemContent += "\n\n기존에 작성된 요약본이 있습니다. 이 형식을 유지하면서 대화 내용을 갱신해주세요:\n" + existingNote;
            }
            
            if ("moderator".equals(roleKey)) {
                // 사회자 프롬프트에 현재 라운드 정보 추가
                if (currentRound > 0 && maxRounds > 0) {
                    systemContent = systemContent.replace("{currentRound}", String.valueOf(currentRound));
                    systemContent = systemContent.replace("{maxRounds}", String.valueOf(maxRounds));
                    
                    // 라운드 진행률에 따른 추가 안내
                    double progress = (double) currentRound / maxRounds;
                    if (progress >= 0.8) {
                        systemContent += "\n\n⚠️ 경고: 최대 라운드의 80% 이상 진행되었습니다. 반드시 종료를 고려하세요.";
                    } else if (progress >= 0.7) {
                        systemContent += "\n\n💡 안내: 최대 라운드의 70% 이상 진행되었습니다. 종료를 적극적으로 고려하세요.";
                    }
                }
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
        
        // researcher 역할일 때 모더레이터가 제안한 검색어로 웹 검색 수행
        if ("researcher".equals(roleKey)) {
            System.out.println("\n[Researcher 역할 활성화] 웹 검색을 수행합니다.");
            
            // userQuestion은 모더레이터가 제안한 단일 검색어
            String searchQuery = userQuestion.trim();
            
            if (searchQuery.isEmpty()) {
                System.out.println("[경고] 모더레이터가 검색어를 제안하지 않았습니다. 원본 질문을 사용합니다.");
                // 히스토리에서 원본 사용자 질문 찾기
                String originalQuestion = history.stream()
                    .filter(chat -> "user".equals(chat.getSender()))
                    .reduce((first, second) -> second)
                    .map(ChatEntity::getMessage)
                    .orElse(searchQuery);
                searchQuery = originalQuestion;
            }
            
            System.out.println("[모더레이터 제안 검색어] " + searchQuery);
            
            try {
                // 웹 검색 수행 (5개 결과 요청)
                String searchResults = googleCustomSearchService.formatSearchResults(searchQuery, 5);
                
                // 검색 결과를 Researcher 에이전트에게 전달
                user.put("content", "다음 검색어로 웹 검색이 수행되었습니다: " + searchQuery + "\n\n" + searchResults);
                System.out.println("[Researcher] 검색 결과를 GPT 프롬프트에 포함했습니다.");
            } catch (Exception e) {
                System.err.println("[Researcher 검색 실패] " + e.getMessage());
                e.printStackTrace();
                // 검색 실패 시 검색어만 전달
                user.put("content", "다음 검색어로 웹 검색을 수행해주세요: " + searchQuery + "\n\n(검색 결과를 가져오는 중 오류가 발생했습니다.)");
            }
        } else {
            user.put("content", userQuestion);
        }
        
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

    private String summarize(List<ChatEntity> history, ChatRoom chatRoom, String existingNote) {
        if (history.isEmpty()) return "요약할 대화 내용이 없습니다.";

        String summarizerPrompt = promptLoader.getPrompt("summarizer");
        if (summarizerPrompt == null) return "요약 프롬프트를 찾을 수 없습니다.";

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        
        // 기존 노트가 있으면 형식 유지하도록 지시
        if (existingNote != null && !existingNote.trim().isEmpty()) {
            summarizerPrompt += "\n\n기존에 작성된 요약본이 있습니다. 이 형식을 유지하면서 대화 내용을 갱신해주세요:\n" + existingNote;
        }
        
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
            summary.setChatRoom(chatRoom);
            // 통계 업데이트
            if (summary.getTokensUsed() != null && summary.getTokensUsed() > 0) {
                chatRoom.addTokensUsed(summary.getTokensUsed());
            }
            chatRoom.incrementRoleParticipation("summarizer");
            chatRepository.save(summary);
            return summary.getMessage();
        }
        return "요약 생성에 실패했습니다.";
    }
    
    // 대화 기록 조회
    @Transactional(readOnly = true)
    public List<ChatEntity> getChatHistory(Long chatRoomId, Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        ChatRoom chatRoom = chatRoomRepository.findByIdAndMember(chatRoomId, member)
                .orElseThrow(() -> new RuntimeException("대화방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        return chatRepository.findByChatRoomOrderByTimestampAsc(chatRoom);
    }
    
    // 통계 조회
    @Transactional(readOnly = true)
    public Map<String, Object> getChatRoomStatistics(Long chatRoomId, Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        ChatRoom chatRoom = chatRoomRepository.findByIdAndMember(chatRoomId, member)
                .orElseThrow(() -> new RuntimeException("대화방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("roleParticipationCount", chatRoom.getRoleParticipationCount());
        statistics.put("totalTokensUsed", chatRoom.getTotalTokensUsed());
        statistics.put("createdAt", chatRoom.getCreatedAt());
        statistics.put("updatedAt", chatRoom.getUpdatedAt());
        statistics.put("totalMessages", chatRoom.getChats().size());
        
        return statistics;
    }

    @Transactional
    public ChatResponseDTO summarizeConversation(Long chatRoomId, Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        ChatRoom chatRoom = chatRoomRepository.findByIdAndMember(chatRoomId, member)
                .orElseThrow(() -> new RuntimeException("대화방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        List<ChatEntity> chatHistory = chatRepository.findByChatRoomOrderByTimestampAsc(chatRoom);
        String summaryText = summarize(chatHistory, chatRoom, chatRoom.getNote());
        // 노트 업데이트
        chatRoom.setNote(summaryText);
        chatRoomRepository.save(chatRoom);
        return new ChatResponseDTO(summaryText);
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