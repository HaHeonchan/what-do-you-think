package com.example.demo.config;

import com.example.demo.dto.PromptDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@Component
public class PromptLoader {
    private Map<String, PromptDTO> prompts;

    @PostConstruct
    public void init() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = TypeReference.class.getResourceAsStream("/prompts.json");

        if (inputStream == null) {
            System.err.println("prompts.json 파일을 찾을 수 없습니다.");
            this.prompts = Collections.emptyMap();
            return;
        }

        // 3. JSON 구조에 맞춰 파싱 타입을 Map<String, Map<String, Prompt>>로 변경
        //    (최상위 키: "prompts", 내부 키: "creator", "critic" 등, 값: Prompt 객체)
        Map<String, Map<String, PromptDTO>> loadedData = objectMapper.readValue(inputStream, new TypeReference<>() {});

        // "prompts" 키에 해당하는 내부 Map을 prompts 필드에 저장
        this.prompts = loadedData.getOrDefault("prompts", Collections.emptyMap());
        System.out.println("JSON 파일로부터 프롬프트 객체를 성공적으로 로드했습니다.");
    }

    // 4. 반환 타입을 Map<String, Prompt>로 변경
    public Map<String, PromptDTO> getPrompts() {
        return this.prompts;
    }
}