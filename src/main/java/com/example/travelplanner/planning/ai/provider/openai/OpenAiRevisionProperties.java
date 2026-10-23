package com.example.travelplanner.planning.ai.provider.openai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "travelplanner.ai.openai")
public class OpenAiRevisionProperties {
    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com";
    private String model = "gpt-6-luna";
    private String reasoningEffort = "low";
    private int connectTimeoutMillis = 2000;
    private int requestTimeoutMillis = 15000;
    private int maxRetries = 2;
    private long retryBackoffMillis = 200;
    private int maxOutputTokens = 1200;
    private int maxCommands = 5;
    private int maxPromptCharacters = 30000;
}
