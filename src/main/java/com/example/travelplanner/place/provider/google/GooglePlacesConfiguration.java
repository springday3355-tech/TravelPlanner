package com.example.travelplanner.place.provider.google;

import com.example.travelplanner.place.provider.PlaceProvider;
import com.example.travelplanner.place.provider.UnavailablePlaceProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(GooglePlacesProperties.class)
public class GooglePlacesConfiguration {

    @Bean
    public PlaceProvider placeProvider(GooglePlacesProperties properties, ObjectMapper objectMapper) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return new UnavailablePlaceProvider();
        }
        return new GooglePlacesProvider(properties, objectMapper);
    }
}
