package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "chats")
public class ChatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @JsonIgnoreProperties({"chats", "member"})
    private ChatRoom chatRoom;

    @Lob // long messages for AI
    private String message;

    private String sender;

    private String timestamp;

    // 토큰 사용량 (이 메시지 생성에 사용된 토큰)
    private Long tokensUsed;
}
