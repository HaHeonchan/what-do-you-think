package com.example.demo.service;

import com.example.demo.entity.Member;
import com.example.demo.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Member createMember(String username, String email) {
        if (memberRepository.existsByUsername(username)) {
            throw new RuntimeException("이미 존재하는 사용자명입니다.");
        }
        
        // 게스트 사용자용 임시 비밀번호 생성
        String tempPassword = passwordEncoder.encode("guest_" + System.currentTimeMillis());
        
        Member member = Member.builder()
                .username(username)
                .email(email)
                .password(tempPassword)
                .build();
        
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Optional<Member> getMember(Long id) {
        return memberRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Member> getMemberByUsername(String username) {
        return memberRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }
}

