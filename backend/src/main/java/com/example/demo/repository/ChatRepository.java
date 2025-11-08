package com.example.demo.repository;

import com.example.demo.entity.ChatEntity;
import com.example.demo.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {
    List<ChatEntity> findByChatRoomOrderByTimestampAsc(ChatRoom chatRoom);
}
