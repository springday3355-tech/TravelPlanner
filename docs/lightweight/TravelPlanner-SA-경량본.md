# TravelPlanner — SA 경량본

> 전체 버전: `docs/architecture/TravelPlanner-SA.md`. 빠르게 참조할 때만 이 문서를 쓰고,
> 애매하면 전체 버전을 확인한다.

- 상태: 0단계 안정화 + 회원/일정/장소 CRUD 완료
- 순서: 여행 계획 최소 흐름 → 사용자 검증 → 필요한 인프라 → 실시간 협업

## 스택
Java 21 · Spring Boot 4.1.0 · Gradle · JPA(H2 로컬/MySQL 운영 예정) · Spring Security/JWT · OpenFeign(미사용) · Lombok

**Boot 4.1 주의:** 테스트 자동구성 패키지가 기능별 모듈로 이동함 · `@MockBean`→`@MockitoBean` · Jackson 3(`tools.jackson.*`) 사용. 2026-09-23 기준 `./gradlew test` 통과.

## 패키지 구조
```
common/  { response, exception, entity, config, health }   ← 공용, 재구현 금지
auth/ itinerary/ place/ realtime/                            ← 각각 controller/service/repository/entity/dto
```

## 레이어 규칙
`Controller → Service → Repository → Entity` (역방향 금지)
- Controller: `@Valid` + `ApiResponse<T>`만 반환, 로직 없음
- Service: `BusinessException`만 던짐, 트랜잭션 경계
- Repository: 쿼리만, 로직 없음
- Entity: `BaseTimeEntity` 상속, setter 대신 의도 메서드

## 공통 골격 (구현됨, 그대로 재사용)
| 클래스 | 경로 | 용도 |
|---|---|---|
| `ApiResponse<T>` | common/response | `success(data)` / `success()`, 실패는 핸들러가 자동 생성 |
| `ErrorCode` / `BusinessException` / `GlobalExceptionHandler` | common/exception | 서비스는 `throw new BusinessException(ErrorCode.X)`만 |
| `BaseTimeEntity` + `JpaAuditingConfig` | common/entity, config | 모든 엔티티 상속, createdAt/updatedAt 자동 |
| `SecurityConfig` | common/config | JWT STATELESS, 회원가입·로그인·공유 조회·장소 조회 공개 |
| `HealthCheckController` | common/health | 골격 동작 확인용 표본, 복제 금지 |

에러코드 접두사: 공통 `C0xx` · 인증 `A0xx`(예정 `M0xx`) · 일정 `P0xx` · 장소 `L0xx` · 실시간 `R0xx`

## 테스트 하네스 (구현됨)
| 베이스 | 용도 |
|---|---|
| `ControllerTestSupport` | MockMvc 컨트롤러 테스트 |
| `IntegrationTestSupport` | 실포트 E2E / WebSocket |
| `RepositoryTestSupport` | `@DataJpaTest` 슬라이스 |

테스트 DB는 `src/test/resources/application.properties`(H2, MODE=MySQL) 고정, 개별 재정의 금지.

## 결정 대기 (임의 구현 금지)
- [x] 인증 방식 — JWT STATELESS 구현 완료
- [ ] MySQL 운영 datasource
- [ ] 검색엔진 선택(ES vs Meilisearch) — 2단계
- [ ] 분산 락 도구 — 3단계

## 로드맵
0단계 안정화 · 1단계 일정 초안 저장 · 2단계 좌표/영업시간/예산 기반 생성 완료 · 다음은 외부 장소/도로 이동시간/AI 수정 공급자 결정 · 이후 WebSocket+낙관적 락
