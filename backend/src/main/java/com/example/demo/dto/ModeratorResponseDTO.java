package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ModeratorResponseDTO {
    private List<String> roleKey;
    private String messages;
}
