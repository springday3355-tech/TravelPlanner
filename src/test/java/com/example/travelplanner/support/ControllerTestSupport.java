package com.example.travelplanner.support;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 계층 테스트 공통 베이스.
 * 전체 스프링 컨텍스트를 로드한 뒤 MockMvc로 HTTP 요청/응답만 검증한다.
 *
 * 주의: Spring Boot 4.x부터 @SpringBootTest가 MockMvc를 자동 구성하지 않으므로
 * @AutoConfigureMockMvc를 명시해야 한다.
 *
 * 도메인이 늘어나 컨텍스트 로딩 비용이 커지면 @WebMvcTest(controllers = ...) +
 * @MockitoBean 조합으로 슬라이스 테스트로 전환하는 것을 검토한다.
 */
@AutoConfigureMockMvc
@SpringBootTest
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
