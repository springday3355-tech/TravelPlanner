package com.example.travelplanner.planning.ai.provider.openai;

import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.planning.ai.provider.AiRevisionContext;
import com.example.travelplanner.planning.ai.provider.AiRevisionProposal;
import com.example.travelplanner.planning.ai.provider.ItineraryRevisionProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiRevisionProvider implements ItineraryRevisionProvider {
    public static final String PROMPT_VERSION = "itinerary-revision-v1";
    private static final String INSTRUCTIONS = """
            You revise an existing travel itinerary. Return only commands allowed by the supplied schema.
            Use only day, item, and place IDs present in the input. Keep each updated visit inside the day's
            date and the selected place's opening hours. Avoid overlapping visits and keep the total estimated
            cost within the itinerary budget. UPDATE may replace a place or reschedule an item. REMOVE deletes
            an item. Use at most the requested command limit. If no safe change satisfies the request, return
            an empty commands array and explain why in summary. Never invent IDs.
            """;

    private final OpenAiRevisionProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiRevisionProvider(OpenAiRevisionProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .build());
    }

    OpenAiRevisionProvider(
            OpenAiRevisionProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String name() {
        return "OPENAI";
    }

    @Override
    public String model() {
        return properties.getModel();
    }

    @Override
    public String promptVersion() {
        return PROMPT_VERSION;
    }

    @Override
    public AiRevisionProposal propose(AiRevisionContext context, String instruction) {
        String input = serializeInput(context, instruction);
        if (input.length() > properties.getMaxPromptCharacters()) {
            throw new BusinessException(
                    ErrorCode.AI_REVISION_LIMIT_EXCEEDED,
                    "AI 일정 수정 입력이 허용된 크기를 초과했습니다."
            );
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("instructions", INSTRUCTIONS);
        body.put("input", input);
        body.put("reasoning", Map.of("effort", properties.getReasoningEffort()));
        body.put("max_output_tokens", properties.getMaxOutputTokens());
        body.put("store", false);
        body.put("text", Map.of("format", responseFormat()));
        return send(body);
    }

    private String serializeInput(AiRevisionContext context, String instruction) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "promptVersion", PROMPT_VERSION,
                    "maxCommands", properties.getMaxCommands(),
                    "instruction", instruction,
                    "itinerary", itineraryInput(context),
                    "candidatePlaces", context.candidatePlaces()
            ));
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_FAILED, "AI 요청을 구성할 수 없습니다.");
        }
    }

    private Map<String, Object> itineraryInput(AiRevisionContext context) {
        Map<String, Object> itinerary = new LinkedHashMap<>();
        itinerary.put("id", context.itineraryId());
        itinerary.put("title", context.title());
        itinerary.put("city", context.city());
        itinerary.put("startDate", context.startDate());
        itinerary.put("endDate", context.endDate());
        itinerary.put("budget", context.budget());
        itinerary.put("days", context.days());
        return itinerary;
    }

    private AiRevisionProposal send(Map<String, Object> body) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(properties.getBaseUrl() + "/v1/responses"))
                        .timeout(Duration.ofMillis(properties.getRequestTimeoutMillis()))
                        .header("Authorization", "Bearer " + properties.getApiKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parse(response.body());
                }
                if (isRetryable(response.statusCode()) && attempt <= properties.getMaxRetries()) {
                    backoff(attempt);
                    continue;
                }
                throw new BusinessException(
                        ErrorCode.AI_PROVIDER_FAILED,
                        "AI 일정 수정 서비스가 요청을 처리하지 못했습니다. 상태 코드: " + response.statusCode()
                );
            } catch (BusinessException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.AI_PROVIDER_FAILED, "AI 일정 수정 요청이 중단되었습니다.");
            } catch (IOException | RuntimeException e) {
                if (attempt <= properties.getMaxRetries()) {
                    backoff(attempt);
                    continue;
                }
                throw new BusinessException(ErrorCode.AI_PROVIDER_FAILED, "AI 일정 수정 서비스에 연결할 수 없습니다.");
            }
        }
    }

    private AiRevisionProposal parse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!"completed".equals(root.path("status").asText())) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_FAILED, "AI 응답이 완료되지 않았습니다.");
            }
            for (JsonNode output : root.path("output")) {
                if (!"message".equals(output.path("type").asText())) {
                    continue;
                }
                for (JsonNode content : output.path("content")) {
                    if ("refusal".equals(content.path("type").asText())) {
                        throw new BusinessException(ErrorCode.AI_PROVIDER_FAILED, "AI가 일정 수정 요청을 거부했습니다.");
                    }
                    if ("output_text".equals(content.path("type").asText())) {
                        return objectMapper.readValue(content.path("text").asText(), AiRevisionProposal.class);
                    }
                }
            }
            throw new BusinessException(ErrorCode.AI_PROVIDER_FAILED, "AI 응답에 일정 수정 명령이 없습니다.");
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_FAILED, "AI 일정 수정 응답을 해석할 수 없습니다.");
        }
    }

    private Map<String, Object> responseFormat() {
        Map<String, Object> nullableInteger = Map.of("anyOf", List.of(
                Map.of("type", "integer"), Map.of("type", "null")
        ));
        Map<String, Object> nullableString = Map.of("anyOf", List.of(
                Map.of("type", "string"), Map.of("type", "null")
        ));
        Map<String, Object> commandProperties = new LinkedHashMap<>();
        commandProperties.put("operation", Map.of("type", "string", "enum", List.of("UPDATE", "REMOVE")));
        commandProperties.put("dayId", Map.of("type", "integer"));
        commandProperties.put("itemId", Map.of("type", "integer"));
        commandProperties.put("placeId", nullableInteger);
        commandProperties.put("startTime", nullableString);
        commandProperties.put("endTime", nullableString);
        commandProperties.put("memo", nullableString);
        commandProperties.put("reason", Map.of("type", "string"));

        Map<String, Object> commandSchema = new LinkedHashMap<>();
        commandSchema.put("type", "object");
        commandSchema.put("properties", commandProperties);
        commandSchema.put("required", List.copyOf(commandProperties.keySet()));
        commandSchema.put("additionalProperties", false);

        Map<String, Object> rootProperties = new LinkedHashMap<>();
        rootProperties.put("summary", Map.of("type", "string"));
        rootProperties.put("commands", Map.of(
                "type", "array",
                "items", commandSchema,
                "maxItems", properties.getMaxCommands()
        ));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", rootProperties);
        schema.put("required", List.of("summary", "commands"));
        schema.put("additionalProperties", false);

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "itinerary_revision");
        format.put("strict", true);
        format.put("schema", schema);
        return format;
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(properties.getRetryBackoffMillis() * (1L << Math.min(attempt - 1, 5)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_PROVIDER_FAILED, "AI 일정 수정 재시도가 중단되었습니다.");
        }
    }
}
