package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ModeratorResponseDTO {
    // 새로운 스키마: request 배열 기반
    private List<RequestItem> request;
    // 대화 종료 여부 (true이면 대화를 종료하고 summarizer를 호출)
    private Boolean shouldEnd;

    @Data
    public static class RequestItem {
        private String roleKey;
        private String messages;
    }
}
