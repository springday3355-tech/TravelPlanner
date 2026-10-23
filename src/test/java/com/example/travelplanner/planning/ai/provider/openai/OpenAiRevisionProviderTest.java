package com.example.travelplanner.planning.ai.provider.openai;

import com.example.travelplanner.planning.ai.provider.AiRevisionContext;
import com.example.travelplanner.planning.ai.provider.AiRevisionProposal;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OpenAiRevisionProviderTest {
    @Autowired
    private ObjectMapper objectMapper;
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("Responses API 실패를 재시도하고 strict Structured Output을 해석한다")
    void propose_retriesAndParsesStructuredOutput() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/responses", exchange -> {
            assertEquals("Bearer test-key", exchange.getRequestHeaders().getFirst("Authorization"));
            JsonNode body = objectMapper.readTree(exchange.getRequestBody());
            assertEquals("gpt-6-luna", body.get("model").asText());
            assertEquals("json_schema", body.get("text").get("format").get("type").asText());
            assertTrue(body.get("text").get("format").get("strict").asBoolean());
            assertFalse(body.get("store").asBoolean());
            assertTrue(body.get("input").asText().contains("비가 오면 실내로 바꿔줘"));
            if (calls.incrementAndGet() == 1) {
                respond(exchange, 500, "{}");
                return;
            }
            respond(exchange, 200, """
                    {
                      "status":"completed",
                      "output":[{
                        "type":"message",
                        "content":[{
                          "type":"output_text",
                          "text":"{\\\"summary\\\":\\\"실내 장소로 변경했습니다.\\\",\\\"commands\\\":[]}"
                        }]
                      }]
                    }
                    """);
        });
        server.start();

        AiRevisionProposal proposal = new OpenAiRevisionProvider(properties(), objectMapper).propose(
                new AiRevisionContext(
                        1L, "서울 여행", "서울", LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 1), 100000, List.of(), List.of()
                ),
                "비가 오면 실내로 바꿔줘"
        );

        assertEquals(2, calls.get());
        assertEquals("실내 장소로 변경했습니다.", proposal.summary());
        assertTrue(proposal.commands().isEmpty());
    }

    private OpenAiRevisionProperties properties() {
        OpenAiRevisionProperties properties = new OpenAiRevisionProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setMaxRetries(1);
        properties.setRetryBackoffMillis(1);
        return properties;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
