# TravelPlanner (트래블메이커) — SA (System Architecture)

> 이 문서는 **개발 에이전트가 코드를 작성할 때 참조하는 규칙 문서**다. 사람이 읽는 보고서가 아니라,
> "이 규칙대로 짜라"는 계약(contract) 문서로 취급한다. 새 도메인/API를 추가하기 전에 반드시 이 문서를 먼저 읽고,
> 여기 정의된 패키지 구조·레이어 규칙·공통 골격을 그대로 재사용한다. 규칙을 바꿔야 하면 코드보다 이 문서를 먼저 수정한다.

- 버전: 0.6
- 상태: 기본 CRUD + Google Places·Routes 기반 여행 계획 V4 구현 완료
- 최종 수정: 2026-10-23

---

## 1. 프로젝트 개요

- 서비스명(기획): 트래블메이커 — 여행 조건(도시/날짜/예산/취향)을 입력하면 AI가 일정을 자동 생성하고,
  실시간 채팅으로 즉시 수정하며, 동행자와 하나의 일정을 실시간 공동 편집하는 여행 플래너.
- 리포지토리명: TravelPlanner
- 현재 단계: 공통 골격 + 회원/일정/장소 CRUD + Google Places 장소와 Google Routes 이동시간을 반영한 일정 초안 생성 구현 완료.
- 다음 작업: 대화형 AI 일정 수정 흐름을 구현한다.

## 2. 기술 스택 (build.gradle 기준, 임의 추측 금지)

| 항목 | 값 |
|---|---|
| 언어 | Java 21 (toolchain 고정) |
| 프레임워크 | Spring Boot 4.1.0 |
| 빌드 | Gradle (wrapper 9.5.1) |
| 영속성 | Spring Data JPA + H2(로컬/테스트), MySQL(운영 예정, 미설정) |
| 보안 | Spring Security + JWT(io.jsonwebtoken:jjwt 0.13.0, jjwt-gson 사용 — Jackson 2/3 충돌 회피), STATELESS |
| 외부 연동 | Spring Cloud OpenFeign (2025.1.2 BOM, 아직 미사용) |
| 롬복 | Lombok |
| 테스트 | JUnit5, spring-boot-starter-data-jpa-test / -security-test / -webmvc-test |

**Boot 4.1 / Security 7 관련 주의사항 (실제로 확인된 변경점, 코드 작성 시 반드시 준수):**
- `@SpringBootTest`가 더 이상 MockMvc를 자동 구성하지 않는다 → `@AutoConfigureMockMvc`를 명시해야 함.
- `@MockBean`/`@SpyBean`은 제거됨 → `@MockitoBean`/`@MockitoSpyBean` 사용.
- `SecurityFilterChain` + 람다 DSL(`http.csrf(c -> c.disable())...`)은 그대로 유효. `@EnableWebSecurity`도 그대로 필요(자동 적용 아님).
- Jackson 3(`tools.jackson.*`)을 사용한다.
- `spring-boot-starter-web` 대신 `spring-boot-starter-webmvc`, `spring-boot-starter-test` 대신 기술별 세분화된 test 스타터를 사용하는 구조.

2026-09-23 기준 `./gradlew test` 전체 통과. 도메인 코드를 추가한 뒤 같은 명령으로 재검증한다.

## 3. 패키지 구조 (고정 규칙)

```
com.example.travelplanner
├─ TravelPlannerApplication.java
├─ common/                      ← 전 도메인 공용, 도메인 지식 금지
│  ├─ response/ApiResponse.java
│  ├─ exception/ErrorCode.java
│  ├─ exception/BusinessException.java
│  ├─ exception/GlobalExceptionHandler.java
│  ├─ entity/BaseTimeEntity.java
│  ├─ config/JpaAuditingConfig.java
│  ├─ config/SecurityConfig.java
│  ├─ security/JwtTokenProvider.java
│  ├─ security/JwtAuthenticationFilter.java
│  ├─ security/JwtAuthenticationEntryPoint.java   (인증 실패도 ApiResponse 포맷 유지)
│  ├─ security/JwtAccessDeniedHandler.java        (인가 실패도 ApiResponse 포맷 유지)
│  ├─ security/SecurityUtils.java                 (getCurrentMemberId())
│  └─ health/HealthCheckController.java   (골격 검증용 표본, 실제 도메인 아님)
├─ auth/                        ← 회원/로그인 (구현 완료)
│  ├─ controller / service / repository / entity / dto
├─ itinerary/                   ← 일정 (구현 완료)
├─ place/                       ← 장소/검색 (구현 완료, 검색엔진 연동은 2단계)
├─ planning/                    ← 여행 조건 기반 일정 초안 생성 (`ROUTE_MATRIX_V4`, 장애 시 V2)
└─ realtime/                    ← 실시간/채팅 (2~3단계 이전에는 생성 금지)
```

**새 도메인 추가 규칙:**
- 패키지명은 도메인명 단수형, 소문자 (`auth`, `itinerary`, `place`).
- 도메인 패키지 하위에 `controller`, `service`, `repository`, `entity`, `dto`를 둔다. 이 이름을 임의로 바꾸지 않는다.
- 도메인 간 직접 참조 금지. 다른 도메인 기능이 필요하면 해당 도메인의 `service` 인터페이스만 의존한다.

## 4. 레이어 규칙 (강제)

의존 방향: `Controller → Service → Repository → Entity` (역방향 금지)

| 계층 | 허용 | 금지 |
|---|---|---|
| Controller | `@Valid` 검증, `ApiResponse`로만 응답, DTO 변환 | 비즈니스 로직, Entity 직접 반환 |
| Service | `@Transactional` 경계, 비즈니스 로직 | `BusinessException` 외 커스텀 예외 생성, Controller/HttpServletRequest 참조 |
| Repository | Spring Data JPA 쿼리 메서드, QueryDSL | 비즈니스 로직 |
| Entity | `BaseTimeEntity` 상속, 의도가 드러나는 메서드(`updateTitle()` 등) | Setter 남발, 서비스 로직 포함 |

새 컨트롤러/서비스를 작성할 때 이 표를 벗어나는 코드가 보이면 리팩터링 대상으로 표시한다.

## 5. 공통 골격 계약 (이미 구현됨 — 재사용만 할 것, 재구현 금지)

### 5.1 ApiResponse<T>
경로: `common/response/ApiResponse.java`

```java
// 성공
return ApiResponse.success(data);   // ApiResponse<T>
return ApiResponse.success();       // ApiResponse<Void>

// 실패는 GlobalExceptionHandler가 자동 생성하므로 컨트롤러/서비스에서 직접 만들지 않는다.
```

응답 JSON 스펙(고정):
```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { },
  "timestamp": "2026-08-01T12:00:00"
}
```
모든 컨트롤러 메서드의 반환 타입은 `ApiResponse<T>` (또는 `ResponseEntity<ApiResponse<T>>`)여야 한다. 도메인 객체나 DTO를 그대로 반환하지 않는다.

### 5.2 예외 처리
경로: `common/exception/{ErrorCode, BusinessException, GlobalExceptionHandler}.java`

- 서비스 계층에서 실패를 알릴 때는 항상 `throw new BusinessException(ErrorCode.XXX)` 를 사용한다. 컨트롤러에서 try-catch 하지 않는다.
- 새 도메인 에러 코드를 추가할 때는 `ErrorCode`에 접두사 규칙을 따른다: 공통 `C0xx`, 인증 `A0xx`. 새 도메인은 다음 접두사를 사용:
  - 회원(auth): `M0xx`
  - 일정(itinerary): `P0xx`
  - 장소(place): `L0xx`
  - 여행 계획(planning): `G0xx`
  - 실시간(realtime): `R0xx`
- 검증 실패(`@Valid` 실패, `MethodArgumentNotValidException`)는 이미 `GlobalExceptionHandler`가 처리하므로 컨트롤러에서 별도 처리하지 않는다.

### 5.3 BaseTimeEntity
경로: `common/entity/BaseTimeEntity.java` + `common/config/JpaAuditingConfig.java`

모든 도메인 엔티티는 이 클래스를 상속한다:
```java
@Entity
public class Itinerary extends BaseTimeEntity {
    // createdAt, updatedAt은 자동 관리되므로 직접 선언하지 않는다.
}
```

### 5.4 SecurityConfig — JWT 인증 확정 구현
경로: `common/config/SecurityConfig.java` + `common/security/*`

- **인증 방식은 JWT(STATELESS)로 확정되었다.** 로그인 성공 시 `common/security/JwtTokenProvider`가 액세스 토큰을 발급하고, 클라이언트는 이후 요청에 `Authorization: Bearer {token}` 헤더를 실어 보낸다.
- `common/security/JwtAuthenticationFilter`가 매 요청마다 헤더를 검사해 유효하면 `memberId`(Long)를 principal로 하는 `Authentication`을 `SecurityContext`에 채운다. 컨트롤러/서비스는 `SecurityUtils.getCurrentMemberId()`로만 현재 로그인 회원을 얻는다(SecurityContextHolder 직접 접근 금지).
- 인증 실패(401)/인가 실패(403)도 `JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler`가 `ApiResponse` 포맷으로 응답한다 — Security 필터 체인 단계의 거부는 `GlobalExceptionHandler`를 거치지 않으므로 이 두 핸들러가 없으면 포맷이 깨진다.
- `PERMIT_ALL_PATHS`: `/api/health`, `/h2-console/**`, `/api/auth/signup`, `/api/auth/login`, `/api/itineraries/shared/**`. `PERMIT_ALL_GET_PATHS`(GET만 허용): `/api/places`, `/api/places/*`.
- `jwt.secret`, `jwt.access-token-validity-seconds`는 `application.properties`에 있다. **운영 배포 전 반드시 별도 secret으로 교체할 것** (현재 로컬 개발용 하드코딩 값).
- 비밀번호는 `BCryptPasswordEncoder`로 해시한다(`SecurityConfig`의 `passwordEncoder` 빈).
- 장소(place) 등록/수정/삭제는 현재 "인증된 사용자면 누구나 가능"하다 — ADMIN 한정 여부는 여전히 결정 대기(정책 문서 3절, API명세 5절)이며, 임의로 제한을 추가하지 않았다.

### 5.5 표본: HealthCheckController
경로: `common/health/HealthCheckController.java`, `GET /api/health`

골격 전체(Security 통과 → Controller → ApiResponse → JSON)가 맞물려 동작하는지 확인하는 용도. 실제 도메인 컨트롤러가 아니므로 복제하지 말고 참고만 한다.

## 6. 테스트 하네스 사용법 (이미 구현됨)

경로: `src/test/java/com/example/travelplanner/support/`

| 베이스 클래스 | 언제 쓰나 | 상속 예시 |
|---|---|---|
| `ControllerTestSupport` | 컨트롤러 계층(MockMvc) 테스트 | `class ItineraryControllerTest extends ControllerTestSupport { ... }` |
| `IntegrationTestSupport` | 실제 포트로 띄운 E2E API 테스트, WebSocket 테스트 | `class ItineraryE2ETest extends IntegrationTestSupport { ... }` |
| `RepositoryTestSupport` | JPA Repository 슬라이스 테스트 | `class ItineraryRepositoryTest extends RepositoryTestSupport { ... }` |

새 도메인 CRUD를 만들 때마다:
1. 컨트롤러 테스트는 `ControllerTestSupport` 상속 후 `mockMvc.perform(...)`으로 작성.
2. 레포지토리 테스트는 `RepositoryTestSupport` 상속 후 `@Autowired TestEntityManager em` 필요 시 직접 주입.
3. 테스트 전용 DB 설정은 `src/test/resources/application.properties`(H2, `MODE=MySQL`)를 그대로 쓴다. 개별 테스트에서 datasource를 재정의하지 않는다.

참고 구현: `common/health/HealthCheckControllerTest.java`(골격 검증용), `auth/controller/AuthControllerTest.java`, `place/controller/PlaceControllerTest.java`, `itinerary/controller/ItineraryControllerTest.java`, `auth/repository/MemberRepositoryTest.java` — 실제 도메인 CRUD에 하네스를 적용한 예시.

## 7. 도메인 모듈 & 1단계 CRUD 순서

| 순서 | 도메인 | 핵심 엔드포인트 | 상태 |
|---|---|---|---|
| 1 | auth (회원/로그인) | `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/members/me` | 구현 완료 |
| 2 | itinerary (일정) | `docs/architecture/TravelPlanner-API명세.md` 2절 전체 | 구현 완료 |
| 3 | place (장소/검색) | `GET/POST/PUT/DELETE /api/places` | 구현 완료 (검색엔진 연동 전, JPA 쿼리 필터링) |
| 4 | planning (여행 계획) | `POST /api/travel-plans/generate` | Google Routes 이동시간 기반 V4 구현 완료 |
| 5 | realtime (실시간/채팅) | WebSocket 엔드포인트 | 여행 계획 흐름 검증 전 착수 금지, 미착수 |

## 8. 단계적 로드맵

- **0단계 (완료)**: Spring Boot 4.1 호환, 핵심 권한·일정 검증, 문서 동기화, 전체 테스트 통과.
- **1단계 (완료)**: 공통 골격/테스트 하네스 + 회원·일정·장소 기본 CRUD (JWT 인증, H2 로컬).
- **1단계 여행 계획 (완료)**: 여행 조건 입력 → 장소 후보 → 규칙 기반 일정 초안 → 검증 → 저장.
- **2단계 일정 품질 (완료)**: 좌표 기반 동선, 영업시간, 예산, 체류시간 반영과 품질 경고, 방문별 이동·비용 추정값 영속화.
- **3~4단계 (완료)**: Google Places 장소 공급자와 Google Routes 이동시간 행렬을 연동하고 장애 대체를 검증했다.
- **5단계**: 구조화 출력과 검증을 거치는 대화형 AI 일정 수정을 구현한다.
- **실시간 단계**: WebSocket 공동 편집, 낙관적 락(`@Version`), 필요 시 Redis Pub/Sub와 분산 락을 추가한다.

## 9. 결정 대기 / 에이전트가 임의로 정하면 안 되는 것

- [x] ~~인증 방식: 세션 vs JWT~~ → JWT(STATELESS)로 확정, `common/security/*` 구현 완료.
- [ ] MySQL 운영 환경 datasource 설정 (현재 로컬은 H2 auto-config만 사용 중).
- [ ] 검색엔진(Elasticsearch vs Meilisearch) 선택 — 외부 장소 데이터 규모와 검색 품질 요구가 확정될 때.
- [ ] 분산 락 도구(Redisson 등) — 3단계 착수 시.
- [ ] 장소(place) 등록/수정/삭제 권한 범위(ADMIN 한정 여부) — 현재 인증된 사용자 누구나 가능(정책 문서 3절).

에이전트는 위 항목을 임의로 구현하지 말고, 구현이 필요해지면 이 문서에 결정 사항을 먼저 기록하도록 사람에게 요청한다.

## 10. 알려진 리스크 (기획서 기반)

- 동시 편집 정합성: 동행자 동시 수정 시 덮어쓰기로 인한 데이터 유실 → 낙관적 락 + 편집 단위 세분화(3단계).
- MySQL(마스터) ↔ 검색엔진 인덱스 동기화 일관성 (검색엔진 도입 시 CDC/배치 방식 결정 필요).
- Elasticsearch/Redis 인프라 + 외부 AI API 호출 비용.
