package com.example.demo.repository;

import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByMemberOrderByUpdatedAtDesc(Member member);
    Optional<ChatRoom> findByIdAndMember(Long id, Member member);
}

