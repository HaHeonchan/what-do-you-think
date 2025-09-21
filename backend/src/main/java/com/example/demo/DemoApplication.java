package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
        System.setProperty("OPENAI_API_KEY", dotenv.get("OPENAI_API_KEY"));
		SpringApplication.run(DemoApplication.class, args);
	}

}
