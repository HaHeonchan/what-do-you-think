package com.example.demo.controller;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.Member;
import com.example.demo.service.ChatRoomService;
import com.example.demo.service.ChatService;
import com.example.demo.util.SecurityUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class GptController {

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final SecurityUtil securityUtil;

    public GptController(ChatService chatService, ChatRoomService chatRoomService, SecurityUtil securityUtil) {
        this.chatService = chatService;
        this.chatRoomService = chatRoomService;
        this.securityUtil = securityUtil;
    }

    // 멤버 관련 엔드포인트 (인증 필요)
    @GetMapping("/members/me")
    public ResponseEntity<?> getCurrentMember() {
        try {
            Member member = securityUtil.getCurrentMember();
            // 비밀번호 제외
            member.setPassword(null);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 대화방 관련 엔드포인트 (인증 필요)
    @PostMapping(value = "/chat-rooms", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createChatRoom(@RequestBody Map<String, Object> request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            String title = request.get("title") != null ? request.get("title").toString() : null;
            
            ChatRoom chatRoom = chatRoomService.createChatRoom(userId, title);
            return ResponseEntity.ok(chatRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/chat-rooms")
    public ResponseEntity<?> getChatRoomsByUser() {
        try {
            Long userId = securityUtil.getCurrentUserId();
            List<ChatRoom> chatRooms = chatRoomService.getChatRoomsByUser(userId);
            return ResponseEntity.ok(chatRooms);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/chat-rooms/{chatRoomId}")
    public ResponseEntity<?> getChatRoom(@PathVariable Long chatRoomId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            return chatRoomService.getChatRoom(chatRoomId, userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // GPT 대화 관련 엔드포인트 (인증 필요)
    @PostMapping(value = "/gpt/question", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponseDTO> sendQuestion(@RequestBody ChatRequestDTO requestDto) {
        try {
            // 현재 사용자 ID로 설정
            Long userId = securityUtil.getCurrentUserId();
            requestDto.setUserId(userId);
            
            ChatResponseDTO response = chatService.askQuestion(requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponseDTO("오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/gpt/summarize")
    public ResponseEntity<ChatResponseDTO> summarizeConversation(@RequestParam Long chatRoomId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            ChatResponseDTO response = chatService.summarizeConversation(chatRoomId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponseDTO("요약 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 대화 기록 조회
    @GetMapping("/chat-rooms/{chatRoomId}/history")
    public ResponseEntity<?> getChatHistory(@PathVariable Long chatRoomId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            List<com.example.demo.entity.ChatEntity> history = chatService.getChatHistory(chatRoomId, userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 통계 조회
    @GetMapping("/chat-rooms/{chatRoomId}/statistics")
    public ResponseEntity<?> getChatRoomStatistics(@PathVariable Long chatRoomId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            Map<String, Object> statistics = chatService.getChatRoomStatistics(chatRoomId, userId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 노트 수정
    @PutMapping(value = "/chat-rooms/{chatRoomId}/note", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateChatRoomNote(
            @PathVariable Long chatRoomId,
            @RequestBody Map<String, String> request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            String note = request.get("note");
            ChatRoom chatRoom = chatRoomService.updateChatRoomNote(chatRoomId, userId, note);
            return ResponseEntity.ok(chatRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 대화방 제목 수정
    @PutMapping(value = "/chat-rooms/{chatRoomId}/title", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateChatRoomTitle(
            @PathVariable Long chatRoomId,
            @RequestBody Map<String, String> request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            String title = request.get("title");
            ChatRoom chatRoom = chatRoomService.updateChatRoomTitle(chatRoomId, userId, title);
            return ResponseEntity.ok(chatRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}