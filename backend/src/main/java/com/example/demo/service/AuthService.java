package com.example.demo.service;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.RegisterRequestDTO;
import com.example.demo.entity.Member;
import com.example.demo.repository.MemberRepository;
import com.example.demo.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        // 사용자명 중복 확인
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 존재하는 사용자명입니다.");
        }

        // 이메일 중복 확인 (이메일이 제공된 경우)
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            memberRepository.findByEmail(request.getEmail()).ifPresent(m -> {
                throw new RuntimeException("이미 존재하는 이메일입니다.");
            });
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 회원 생성
        Member member = Member.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)
                .build();

        member = memberRepository.save(member);

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(member.getId(), member.getUsername());

        return new AuthResponseDTO(token, member.getId(), member.getUsername(), member.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        // 사용자 조회
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자명 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new RuntimeException("사용자명 또는 비밀번호가 올바르지 않습니다.");
        }

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(member.getId(), member.getUsername());

        return new AuthResponseDTO(token, member.getId(), member.getUsername(), member.getEmail());
    }
}

