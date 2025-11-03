package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ModeratorResponseDTO {
    // 새로운 스키마: request 배열 기반
    private List<RequestItem> request;

    @Data
    public static class RequestItem {
        private String roleKey;
        private String messages;
    }
}
