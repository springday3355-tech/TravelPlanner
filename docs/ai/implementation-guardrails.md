# Implementation Guardrails

> 개발 에이전트가 스스로 결정하면 안 되는 것과, 항상 지켜야 하는 하드 제약. 편의를 위해 임의로 완화하지 않는다.

## 1. 절대 임의로 결정하지 않는다 (사람 확인 필수)

- ~~인증 방식(세션 vs JWT)~~ — JWT로 확정, 구현 완료(`docs/architecture/TravelPlanner-SA.md` 5.4절)
- 검색엔진 선택(Elasticsearch vs Meilisearch) — 2단계 착수 전
- 분산 락 도구(Redisson 등) — 3단계 착수 전
- MySQL 운영 datasource 설정
- `docs/domain/트래블메이커-정책정리본.md`에 "결정 대기"로 표시된 모든 항목
- 2단계/3단계 기능(검색엔진 연동, Redis 캐싱, WebSocket, 낙관적/분산 락)을 1단계 범위에서 미리 구현하는 것 — `docs/product/TravelPlanner-PRD.md`의 단계 구분을 넘어서지 않는다.

## 2. 항상 지키는 하드 규칙

- 컨트롤러는 `ApiResponse<T>` 외의 타입을 직접 반환하지 않는다.
- 서비스 계층은 `BusinessException` 외의 커스텀 런타임 예외를 새로 만들지 않는다. 새 실패 케이스는 `ErrorCode`에 추가한다.
- 모든 엔티티는 `common/entity/BaseTimeEntity`를 상속한다.
- 레이어 의존 방향(`Controller → Service → Repository → Entity`)을 역행하지 않는다.
- `build.gradle`의 의존성을 새로 추가/변경해야 하면, 먼저 이유를 설명하고 사람의 확인을 받는다(특히 Redis/Elasticsearch/WebSocket 등 2~3단계 의존성을 1단계에서 미리 추가하지 않는다).
- `src/test/resources/application.properties`(테스트 DB 설정)를 개별 테스트에서 재정의하지 않는다.
- 기존 공통 골격(`ApiResponse`, `GlobalExceptionHandler`, `BaseTimeEntity`, `SecurityConfig`, 테스트 하네스 3종)을 다시 구현하지 않는다 — 반드시 재사용한다.

## 3. 문서와 코드가 어긋날 때

- 코드를 문서에 맞추는 것이 기본값이다.
- 문서가 잘못됐다고 판단되면, 코드부터 바꾸지 말고 어떤 문서의 어느 부분이 왜 틀렸는지 먼저 사람에게 보고한다.

## 4. 빌드/검증

- 이 리포지토리는 일부 개발 환경(샌드박스)에서 `./gradlew test`가 네트워크 제약으로 동작하지 않을 수 있다. 그 경우에도 "테스트를 통과했다"고 보고하지 않는다 — 실행하지 못했다면 그렇게 명시한다.
- 실제로 빌드/테스트를 실행할 수 있는 환경이면, 코드 변경 후 반드시 `./gradlew test`를 실행하고 결과를 보고한다.
