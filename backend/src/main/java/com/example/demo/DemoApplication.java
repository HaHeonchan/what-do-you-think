package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		// .env 파일 로드 (프로젝트 루트 또는 /app 디렉토리에서 찾음)
		Dotenv dotenv = null;
		try {
			// 먼저 현재 작업 디렉토리(프로젝트 루트)에서 찾기
			String currentDir = System.getProperty("user.dir");
			dotenv = Dotenv.configure()
					.directory(currentDir)
					.ignoreIfMissing()
					.load();
			
			// Docker 컨테이너 내부에서는 /app에서도 찾기 시도
			if (dotenv.get("OPENAI_API_KEY", null) == null) {
				try {
					dotenv = Dotenv.configure()
							.directory("/app")
							.ignoreIfMissing()
							.load();
				} catch (Exception e) {
					// 무시
				}
			}
		} catch (Exception e) {
			// .env 파일이 없어도 계속 진행
			System.err.println("Info: .env file not found. Using environment variables or application.yml");
		}
		
		// 환경 변수가 이미 설정되어 있으면 사용, 없으면 .env 파일에서 로드
		String openAiApiKey = System.getenv("OPENAI_API_KEY");
		if ((openAiApiKey == null || openAiApiKey.isEmpty()) && dotenv != null) {
			openAiApiKey = dotenv.get("OPENAI_API_KEY", null);
		}
		if (openAiApiKey != null && !openAiApiKey.isEmpty()) {
			System.setProperty("OPENAI_API_KEY", openAiApiKey);
		}
		
		// JWT_SECRET은 환경 변수에서 필수로 가져옴 (보안)
		String jwtSecret = System.getenv("JWT_SECRET");
		if ((jwtSecret == null || jwtSecret.isEmpty()) && dotenv != null) {
			jwtSecret = dotenv.get("JWT_SECRET", null);
		}
		if (jwtSecret == null || jwtSecret.isEmpty()) {
			throw new IllegalStateException(
				"JWT_SECRET environment variable is required. " +
				"Please set JWT_SECRET in your .env file or as an environment variable with at least 32 characters."
			);
		}
		// JWT 시크릿 키는 최소 32자(256비트) 이상이어야 함
		if (jwtSecret.length() < 32) {
			throw new IllegalArgumentException(
				"JWT_SECRET must be at least 32 characters (256 bits). " +
				"Current length: " + jwtSecret.length() + " characters."
			);
		}
		System.setProperty("JWT_SECRET", jwtSecret);
		
		SpringApplication.run(DemoApplication.class, args);
	}

}
