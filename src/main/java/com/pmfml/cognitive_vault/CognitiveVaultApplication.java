package com.pmfml.cognitive_vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CognitiveVaultApplication {

	public static void main(String[] args) {
		System.setProperty("java.io.tmpdir", "target");
		SpringApplication.run(CognitiveVaultApplication.class, args);
	}

}
