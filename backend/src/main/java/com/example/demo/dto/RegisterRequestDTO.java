package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterRequestDTO {
    private String username;
    private String email;
    private String password;
}

