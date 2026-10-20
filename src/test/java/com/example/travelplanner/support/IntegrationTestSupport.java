package com.example.travelplanner.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;

/**
 * 임의 포트로 실제 서버를 띄워 API를 엔드투엔드로 검증하는 통합 테스트 공통 베이스.
 * WebSocket 등 실제 네트워크 계층 검증이 필요한 실시간 도메인 테스트에서 특히 유용하다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;
}
