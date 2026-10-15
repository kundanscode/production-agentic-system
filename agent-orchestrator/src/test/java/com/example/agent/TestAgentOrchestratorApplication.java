package com.example.agent;

import org.springframework.boot.SpringApplication;

public class TestAgentOrchestratorApplication {

	public static void main(String[] args) {
		SpringApplication.from(AgentOrchestratorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
