package com.example.travelplanner.place.provider.google;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "travelplanner.places.google")
public class GooglePlacesProperties {

    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://places.googleapis.com";
    private int connectTimeoutMillis = 2000;
    private int requestTimeoutMillis = 5000;
    private int maxResults = 10;
    private int maxRetries = 2;
    private long retryBackoffMillis = 200;
}
