package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequestDTO {
    private Long userId; // 사용자 ID (서버에서 자동 설정)
    private Long chatRoomId; // 대화방 ID (선택적, 없으면 새로 생성)
    private String question;
    private String sender;
    private String receiver;
    private List<String> promptKeys;
    private Integer conversationRounds; // AI끼리의 대화 횟수
}