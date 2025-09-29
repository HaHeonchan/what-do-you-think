package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatRequestDTO {
    private String question;
    private String sender;
    private String receiver;
    private List<String> promptKeys;
}