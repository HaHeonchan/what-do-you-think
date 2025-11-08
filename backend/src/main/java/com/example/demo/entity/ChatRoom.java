package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnoreProperties({"chatRooms"})
    private Member member;

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String note; // 노트 내용 (사용자가 수정 가능)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"chatRoom"})
    @Builder.Default
    private List<ChatEntity> chats = new ArrayList<>();

    // 통계 필드
    @ElementCollection
    @CollectionTable(name = "chat_room_role_participation", joinColumns = @JoinColumn(name = "chat_room_id"))
    @MapKeyColumn(name = "role_key")
    @Column(name = "participation_count")
    @Builder.Default
    private Map<String, Integer> roleParticipationCount = new HashMap<>();

    @Column(nullable = false)
    @Builder.Default
    private Long totalTokensUsed = 0L;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (roleParticipationCount == null) {
            roleParticipationCount = new HashMap<>();
        }
        if (totalTokensUsed == null) {
            totalTokensUsed = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 역할 참여 횟수 증가 헬퍼 메서드
    public void incrementRoleParticipation(String roleKey) {
        if (roleParticipationCount == null) {
            roleParticipationCount = new HashMap<>();
        }
        roleParticipationCount.put(roleKey, roleParticipationCount.getOrDefault(roleKey, 0) + 1);
    }

    // 토큰 사용량 추가 헬퍼 메서드
    public void addTokensUsed(Long tokens) {
        if (totalTokensUsed == null) {
            totalTokensUsed = 0L;
        }
        totalTokensUsed += tokens;
    }
}

