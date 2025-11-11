package com.example.demo.service;

import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.Member;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;

    public ChatRoomService(ChatRoomRepository chatRoomRepository, MemberRepository memberRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public ChatRoom createChatRoom(Long userId, String title) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        ChatRoom chatRoom = ChatRoom.builder()
                .member(member)
                .title(title != null ? title : "새 세션")
                .build();
        
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getChatRoomsByUser(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return chatRoomRepository.findByMemberOrderByUpdatedAtDesc(member);
    }

    @Transactional(readOnly = true)
    public Optional<ChatRoom> getChatRoom(Long chatRoomId, Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return chatRoomRepository.findByIdAndMember(chatRoomId, member);
    }

    @Transactional
    public ChatRoom updateChatRoomTitle(Long chatRoomId, Long userId, String title) {
        ChatRoom chatRoom = getChatRoom(chatRoomId, userId)
                .orElseThrow(() -> new RuntimeException("대화방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        chatRoom.setTitle(title);
        return chatRoomRepository.save(chatRoom);
    }
    
    @Transactional
    public ChatRoom updateChatRoomNote(Long chatRoomId, Long userId, String note) {
        ChatRoom chatRoom = getChatRoom(chatRoomId, userId)
                .orElseThrow(() -> new RuntimeException("대화방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        chatRoom.setNote(note);
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId, userId)
                .orElseThrow(() -> new RuntimeException("대화방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        chatRoomRepository.delete(chatRoom);
    }
}

