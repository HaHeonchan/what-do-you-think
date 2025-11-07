package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		// 환경 변수가 이미 설정되어 있으면 사용, 없으면 .env 파일에서 로드
		String openAiApiKey = System.getenv("OPENAI_API_KEY");
		if (openAiApiKey == null || openAiApiKey.isEmpty()) {
			try {
				Dotenv dotenv = Dotenv.load();
				openAiApiKey = dotenv.get("OPENAI_API_KEY");
			} catch (Exception e) {
				System.err.println("Warning: Could not load .env file. Using environment variables or application.yml");
			}
		}
		if (openAiApiKey != null && !openAiApiKey.isEmpty()) {
			System.setProperty("OPENAI_API_KEY", openAiApiKey);
		}
		SpringApplication.run(DemoApplication.class, args);
	}

}
