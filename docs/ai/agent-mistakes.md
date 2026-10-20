# Agent Mistakes (재발 방지 목록)

> 이 프로젝트에서 에이전트가 반복하기 쉬운 실수. 새로운 실수가 발견되면 이 목록에 추가한다(사후 기록용 문서이므로 항목 삭제보다 갱신 이력 남기기를 우선한다).

## 아키텍처/구조

- 컨트롤러에서 엔티티나 DTO를 `ApiResponse`로 감싸지 않고 바로 반환.
- 새 도메인 패키지를 `controller/service/repository/entity/dto` 구조 없이 파일 몇 개로만 구성.
- 도메인 패키지끼리 서비스 인터페이스가 아닌 리포지토리/엔티티를 직접 참조.
- `BaseTimeEntity`를 상속하지 않고 `createdAt`/`updatedAt`을 엔티티에 직접 선언.
- `common` 패키지에 특정 도메인 지식(예: 일정 관련 로직)을 넣는 것.

## 공통 골격 무시

- 실패를 `BusinessException` 대신 일반 `RuntimeException`이나 `IllegalArgumentException`으로 던짐.
- 새 실패 케이스인데 기존 `ErrorCode`를 억지로 재사용(의미가 안 맞는 코드 재활용).
- `GlobalExceptionHandler`가 있는데도 컨트롤러에서 try-catch로 예외를 직접 처리.

## 범위 침범

- PRD 1단계 범위인데 2~3단계 기술(Redis, Elasticsearch, WebSocket, 분산 락)을 미리 끌어와 구현.
- "결정 대기"로 표시된 항목(인증 방식 등)을 그럴듯한 기본값으로 조용히 확정하고 넘어가는 것.

## 테스트

- 하네스(`ControllerTestSupport`/`IntegrationTestSupport`/`RepositoryTestSupport`)를 쓰지 않고 매번 새로운 테스트 설정을 처음부터 작성.
- 테스트 DB 설정(`src/test/resources/application.properties`)을 개별 테스트에서 재정의.
- `./gradlew test`를 실행하지 않고/실행하지 못했으면서 "테스트 통과"라고 보고.

## Spring Boot 4.1 관련 (실제로 발생했던 함정)

- `@SpringBootTest`만 붙이고 `@AutoConfigureMockMvc`를 빠뜨려 `MockMvc` 빈이 없다고 실패.
- `@MockBean`/`@SpyBean`을 그대로 사용(Boot 4.0에서 제거됨) — `@MockitoBean`/`@MockitoSpyBean` 사용.
- Jackson 3 전환으로 `ObjectMapper` import 경로가 다를 수 있음을 무시하고 컴파일 에러를 방치.

## 문서 관리

- 코드 구조를 바꿨는데 `docs/architecture/TravelPlanner-SA.md`(또는 관련 문서)를 갱신하지 않아 문서와 코드가 어긋남.
- 경량본(`docs/lightweight/`)만 보고 전체본과의 차이를 확인하지 않음.
