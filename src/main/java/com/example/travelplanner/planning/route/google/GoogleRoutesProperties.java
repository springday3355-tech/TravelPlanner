package com.example.travelplanner.planning.route.google;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "travelplanner.routes.google")
public class GoogleRoutesProperties {
    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://routes.googleapis.com";
    private int connectTimeoutMillis = 2000;
    private int requestTimeoutMillis = 5000;
    private int maxRetries = 2;
    private long retryBackoffMillis = 200;
    private int maxElementsPerPlan = 120;
    private long cacheTtlSeconds = 1800;
    private int cacheMaxEntries = 200;
}
