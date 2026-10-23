package com.example.travelplanner.planning.route.google;

import com.example.travelplanner.planning.route.RouteMatrixProvider;
import com.example.travelplanner.planning.route.UnavailableRouteMatrixProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(GoogleRoutesProperties.class)
public class GoogleRoutesConfiguration {
    @Bean
    public RouteMatrixProvider routeMatrixProvider(
            GoogleRoutesProperties properties,
            ObjectMapper objectMapper
    ) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return new UnavailableRouteMatrixProvider();
        }
        return new GoogleRoutesProvider(properties, objectMapper);
    }
}
