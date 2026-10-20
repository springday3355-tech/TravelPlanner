package com.example.travelplanner.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * JPA Repository 계층 테스트 공통 베이스.
 * src/test/resources/application.properties 에 정의된 H2(MODE=MySQL) 설정을 그대로 사용하며,
 * @DataJpaTest 기본값(내장 DB로 자동 치환)을 끄기 위해 Replace.NONE을 지정한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
public abstract class RepositoryTestSupport {
}
