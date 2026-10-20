# TravelPlanner — DB 설계

> 1~2단계(기본 CRUD + 동선·영업시간·예산 기반 일정 생성) 범위의 테이블 설계. 새 엔티티를 만들 때 이 문서와 다르게 컬럼을 추가/변경하면
> 이 문서를 먼저 갱신한다. 모든 테이블은 `common/entity/BaseTimeEntity`를 상속하므로 `created_at`,
> `updated_at` 컬럼은 아래 표에 별도로 적지 않는다(자동 포함).

- 로컬/테스트: H2 (`MODE=MySQL`)
- 운영 예정: MySQL — datasource 설정은 미확정 (`TravelPlanner-SA.md` 9절 참고)

## 1. ERD 개요

```
Member (1) ── (N) Itinerary            : Itinerary.owner_id → Member.id
Member (1) ── (N) ItineraryParticipant : 동행자로 참여
Itinerary (1) ── (N) ItineraryParticipant
Itinerary (1) ── (N) ItineraryDay
ItineraryDay (1) ── (N) ItineraryItem
ItineraryItem (N) ── (1) Place
```

다이어그램(Mermaid): `docs/architecture/TravelPlanner-ERD.md`. 컬럼을 바꿀 때는 이 문서와 ERD 문서를 함께 갱신한다.

## 2. 테이블 정의 (1~2단계 범위)

### 2.1 member (auth 도메인)

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 식별자 |
| password | VARCHAR(255) | NOT NULL | 해시 저장(BCrypt 등, 방식 미확정) |
| nickname | VARCHAR(50) | NOT NULL | |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | `USER`, `ADMIN` |

### 2.2 itinerary (itinerary 도메인)

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| owner_id | BIGINT | FK → member.id, NOT NULL | 일정 생성자 |
| title | VARCHAR(100) | NOT NULL | |
| city | VARCHAR(50) | NOT NULL | 여행 도시/지역 |
| start_date | DATE | NOT NULL | |
| end_date | DATE | NOT NULL | |
| budget | INT | NULL | 단위: 원 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'DRAFT' | `DRAFT`, `CONFIRMED`, `ARCHIVED` |
| share_token | VARCHAR(64) | UNIQUE, NULL | 공유 링크용 토큰. 발급 전 NULL |

> `version`(낙관적 락) 컬럼은 3단계(동시 편집) 착수 시 추가한다. 1단계에서 미리 넣지 않는다 — SA 문서 9절 "결정 대기" 원칙과 동일하게, 필요해지는 시점에 추가한다.

### 2.3 itinerary_day

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| itinerary_id | BIGINT | FK → itinerary.id, NOT NULL | |
| day_number | INT | NOT NULL | 1일차, 2일차 ... |
| date | DATE | NOT NULL | |

- 유니크 제약: (itinerary_id, day_number)

### 2.4 itinerary_item

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| itinerary_day_id | BIGINT | FK → itinerary_day.id, NOT NULL | |
| place_id | BIGINT | FK → place.id, NOT NULL | |
| visit_order | INT | NOT NULL | 하루 내 방문 순서 |
| start_time | TIME | NULL | |
| end_time | TIME | NULL | |
| memo | VARCHAR(500) | NULL | |
| travel_minutes_before | INT | NULL | 직전 위치에서 이 장소까지의 예상 이동시간(분) |
| distance_km_before | DECIMAL(10,2) | NULL | 직전 위치에서 이 장소까지의 예상 거리(km) |
| estimated_cost | INT | NULL | 이 장소에서 사용할 1인 기준 예상 비용(원) |

- 유니크 제약: (itinerary_day_id, visit_order)
- 일정 항목에서 참조 중인 장소는 삭제할 수 없다.

### 2.5 itinerary_participant (동행자)

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| itinerary_id | BIGINT | FK → itinerary.id, NOT NULL | |
| member_id | BIGINT | FK → member.id, NOT NULL | |
| participant_role | VARCHAR(20) | NOT NULL, DEFAULT 'EDITOR' | `OWNER`, `EDITOR`, `VIEWER` |

- 유니크 제약: (itinerary_id, member_id)

### 2.6 place (place 도메인)

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(100) | NOT NULL | |
| category | VARCHAR(30) | NOT NULL | enum 문자열(`FOOD`, `CAFE`, `SIGHT`, `ACCOMMODATION`), 검색 고도화 시 재검토 |
| region | VARCHAR(50) | NOT NULL | |
| address | VARCHAR(255) | NULL | |
| latitude | DECIMAL(10,7) | NULL | |
| longitude | DECIMAL(10,7) | NULL | |
| price_range | VARCHAR(20) | NULL | |
| open_hours | VARCHAR(255) | NULL | 기존 표시용 텍스트 |
| opening_time | TIME | NULL | 2단계 일정 계산용 영업 시작 시각. closing_time과 함께 입력 |
| closing_time | TIME | NULL | 2단계 일정 계산용 영업 종료 시각. 자정 이후 영업은 현재 미지원 |
| estimated_cost | INT | NULL | 1인 기준 예상 비용(원), 0 이상 |
| recommended_visit_minutes | INT | NULL | 권장 체류시간, 30~480분 |

- 위도·경도는 둘 다 입력하거나 둘 다 생략한다.
- 영업 시작·종료 시간은 둘 다 입력하거나 둘 다 생략하며 종료 시간이 더 늦어야 한다.
- 요일별·공휴일별 영업시간이 필요해지면 별도 테이블로 분리한다.

## 3. 인덱스 (제안)

- `itinerary(owner_id)`, `itinerary(share_token)`
- `itinerary_participant(member_id)`
- `place(region, category)` — 1단계 필터링 쿼리 기준

## 4. 결정 대기 (임의로 정하지 말 것)

- [x] 비밀번호 해시 알고리즘 — BCrypt 적용 완료
- [ ] soft delete 여부(삭제 컬럼 추가 vs hard delete) — 현재는 hard delete 가정, 정책 문서에서 재확인
- [ ] place 데이터 적재 방식(직접 입력 vs 외부 API 수집) — 외부 장소 데이터 공급자 선정 시 결정
- [ ] `version`(낙관적 락) 컬럼 도입 시점 — 3단계 착수 시

## 5. 관련 문서

- 구현 규칙: `docs/architecture/TravelPlanner-SA.md`
- 요구사항: `docs/product/TravelPlanner-PRD.md`
- 비즈니스 정책: `docs/domain/트래블메이커-정책정리본.md`
