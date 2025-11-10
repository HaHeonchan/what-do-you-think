package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class PromptLoader {
    private final ResourceLoader resourceLoader;
    private Map<String, String> prompts;
    private Map<String, String> instructions;

    public PromptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException {
        this.prompts = new HashMap<>();
        this.instructions = new HashMap<>();
        
        // 마크다운 파일들 로드
        String[] promptFiles = {"creator.md", "critic.md", "analyst.md", "optimizer.md", "summarizer.md", "moderator.md", "researcher.md"};

        for (String fileName : promptFiles) {
            try {
                Resource resource = resourceLoader.getResource("classpath:prompts/" + fileName);
                if (resource.exists()) {
                    InputStream inputStream = resource.getInputStream();
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                    // 파일명에서 확장자 제거하여 키로 사용
                    String key = fileName.substring(0, fileName.lastIndexOf('.'));
                    prompts.put(key, content);

                    System.out.println("마크다운 파일 로드 성공: " + fileName);
                } else {
                    System.err.println("마크다운 파일을 찾을 수 없습니다: " + fileName);
                }
            } catch (Exception e) {
                System.err.println("마크다운 파일 로드 실패: " + fileName + " - " + e.getMessage());
            }
        }

        System.out.println("총 " + prompts.size() + "개의 마크다운 프롬프트를 로드했습니다.");

        // 지시사항(instructions) 로드
        String[] instructionFiles = {"debate_response.md"};
        for (String fileName : instructionFiles) {
            try {
                Resource resource = resourceLoader.getResource("classpath:prompts/instructions/" + fileName);
                if (resource.exists()) {
                    InputStream inputStream = resource.getInputStream();
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                    String key = fileName.substring(0, fileName.lastIndexOf('.'));
                    instructions.put(key, content);
                    System.out.println("지시사항 파일 로드 성공: " + fileName);
                } else {
                    System.err.println("지시사항 파일을 찾을 수 없습니다: " + fileName);
                }
            } catch (Exception e) {
                System.err.println("지시사항 파일 로드 실패: " + fileName + " - " + e.getMessage());
            }
        }
    }

    public Map<String, String> getPrompts() {
        return this.prompts;
    }
    
    public String getPrompt(String key) {
        return this.prompts.get(key);
    }

    public String getInstruction(String key) {
        return this.instructions.get(key);
    }
}