package com.example.travelplanner.planning.ai.provider.openai;

import com.example.travelplanner.planning.ai.provider.ItineraryRevisionProvider;
import com.example.travelplanner.planning.ai.provider.UnavailableItineraryRevisionProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(OpenAiRevisionProperties.class)
public class OpenAiRevisionConfiguration {
    @Bean
    public ItineraryRevisionProvider itineraryRevisionProvider(
            OpenAiRevisionProperties properties,
            ObjectMapper objectMapper
    ) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return new UnavailableItineraryRevisionProvider();
        }
        return new OpenAiRevisionProvider(properties, objectMapper);
    }
}
