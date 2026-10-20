# TravelPlanner — API 명세 (기본 CRUD + 여행 계획 초안 생성)

> 구현 시 그대로 따르는 계약 문서. 경로/필드명을 임의로 바꾸지 않는다. 바꿔야 하면 이 문서를 먼저 수정한다.
> 참고: `docs/architecture/TravelPlanner-SA.md`(레이어 규칙), `docs/architecture/TravelPlanner-DB설계.md`(테이블),
> `docs/domain/트래블메이커-정책정리본.md`(권한 규칙)

## 0. 공통 규칙

- Base path: `/api`
- 모든 응답은 `common/response/ApiResponse<T>`로 감싼다 (SA 문서 5.1절 포맷 고정).
- 실패 응답의 `code`는 `common/exception/ErrorCode`에 정의된 값만 사용한다.
- 인증이 필요한 요청은 `Authorization: Bearer {accessToken}` 헤더를 사용한다. 로그인 성공 시 JWT 액세스 토큰을 발급한다.
- 날짜: `YYYY-MM-DD`(DATE), 시간: `HH:mm`(TIME). 목록 조회는 페이지네이션을 1단계에서는 생략하고 전체 목록을 반환한다(데이터 양이 늘어나면 이 문서에 페이지네이션 규격을 추가한 뒤 구현).
- 요청 검증 실패, 리소스 없음 등 공통 에러는 SA 문서 5.2절 규칙을 따른다(컨트롤러에서 별도 처리 금지).

## 1. 회원/로그인 (auth)

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/auth/signup` | 회원가입 | 불필요 |
| POST | `/api/auth/login` | 로그인 | 불필요 |
| GET | `/api/members/me` | 내 정보 조회 | 필요 |

### POST /api/auth/signup

요청:
```json
{
  "email": "user@example.com",
  "password": "plain-text-password",
  "nickname": "송이"
}
```
성공 응답 (`data`):
```json
{ "id": 1, "email": "user@example.com", "nickname": "송이" }
```
실패: `ErrorCode.INVALID_INPUT_VALUE`(형식 오류), 이메일 중복 시 신규 코드 `M001`을 `ErrorCode`에 추가해 사용(정책 문서 1절).

### POST /api/auth/login

요청:
```json
{ "email": "user@example.com", "password": "plain-text-password" }
```
성공 응답 (`data`) — **인증 방식이 JWT로 확정되어 구현 완료됨**:
```json
{
  "accessToken": "eyJhbGciOi...",
  "member": { "id": 1, "email": "user@example.com", "nickname": "송이", "role": "USER" }
}
```
이후 요청은 `Authorization: Bearer {accessToken}` 헤더를 실어 보낸다. 실패: 이메일/비밀번호 불일치 시 `ErrorCode.INVALID_CREDENTIALS`(M003, 401).

### GET /api/members/me

성공 응답 (`data`):
```json
{ "id": 1, "email": "user@example.com", "nickname": "송이", "role": "USER" }
```

## 2. 일정 (itinerary)

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/itineraries` | 일정 생성 | 필요 |
| GET | `/api/itineraries` | 내 일정 목록(소유+동행 참여) | 필요 |
| GET | `/api/itineraries/{id}` | 일정 단건 조회 | 필요 |
| PUT | `/api/itineraries/{id}` | 일정 수정 | 필요 (OWNER/EDITOR) |
| DELETE | `/api/itineraries/{id}` | 일정 삭제 | 필요 (OWNER만, 정책 2절) |
| POST | `/api/itineraries/{id}/share` | 공유 링크 발급 | 필요 (OWNER) |
| GET | `/api/itineraries/shared/{shareToken}` | 공유 링크로 조회 | 불필요 (VIEWER 권한, 정책 2절) |
| POST | `/api/itineraries/{id}/participants` | 동행자 초대 | 필요 (OWNER) |
| DELETE | `/api/itineraries/{id}/participants/{participantId}` | 동행자 제거 | 필요 (OWNER) |
| POST | `/api/itineraries/{id}/days` | 일자 추가 | 필요 (OWNER/EDITOR) |
| POST | `/api/itineraries/{id}/days/{dayId}/items` | 일자 내 방문 항목 추가 | 필요 (OWNER/EDITOR) |
| PUT | `/api/itineraries/{id}/days/{dayId}/items/{itemId}` | 방문 항목 수정 | 필요 (OWNER/EDITOR) |
| DELETE | `/api/itineraries/{id}/days/{dayId}/items/{itemId}` | 방문 항목 삭제 | 필요 (OWNER/EDITOR) |

### POST /api/itineraries

요청:
```json
{
  "title": "부산 2박 3일",
  "city": "부산",
  "startDate": "2026-09-01",
  "endDate": "2026-09-03",
  "budget": 500000
}
```
성공 응답 (`data`): `Itinerary` 표현 (아래 4절 DTO 참고), `status`는 서버에서 `DRAFT`로 고정, `ownerId`는 인증 주체에서 채운다(요청 바디로 받지 않는다).

### GET /api/itineraries

성공 응답 (`data`): `ItinerarySummary[]`
```json
[
  { "id": 1, "title": "부산 2박 3일", "city": "부산", "startDate": "2026-09-01", "endDate": "2026-09-03", "status": "DRAFT", "myRole": "OWNER" }
]
```

### GET /api/itineraries/{id}

성공 응답 (`data`): `ItineraryDetail` — Itinerary + `days[]`(각 day는 `items[]` 포함) + `participants[]`. 상세 필드는 4절 DTO 참고.
실패: 존재하지 않으면 `ErrorCode.ENTITY_NOT_FOUND`, 조회 권한 없으면(참여자 아님) `ErrorCode.FORBIDDEN`.

### PUT /api/itineraries/{id}

요청 (부분 수정 아님, 전체 필드 전송):
```json
{ "title": "부산 2박 3일(수정)", "city": "부산", "startDate": "2026-09-01", "endDate": "2026-09-04", "budget": 600000, "status": "CONFIRMED" }
```
실패: 권한 없으면 `ErrorCode.FORBIDDEN`.

### DELETE /api/itineraries/{id}

성공 응답: `ApiResponse.success()` (data 없음). 하위 `days`/`items`는 cascade 삭제(정책 문서 2절).

### POST /api/itineraries/{id}/share

성공 응답 (`data`):
```json
{ "shareToken": "a1b2c3d4..." }
```

### GET /api/itineraries/shared/{shareToken}

성공 응답: `ItineraryDetail`과 동일하되, 편집 관련 필드(participants의 상세 email 등 민감 정보)는 제외. VIEWER 권한 고정(정책 문서 2절, 링크로 EDITOR 부여 여부는 결정 대기).

### POST /api/itineraries/{id}/participants

요청:
```json
{ "memberEmail": "friend@example.com", "participantRole": "EDITOR" }
```
성공 응답 (`data`): `Participant` — `{ "id": 1, "memberId": 2, "nickname": "친구", "participantRole": "EDITOR" }`

초대 가능한 역할은 `EDITOR`, `VIEWER`다. `OWNER`는 초대로 부여할 수 없으며 기존 소유자 참여 정보도 삭제할 수 없다.

### POST /api/itineraries/{id}/days

요청:
```json
{ "dayNumber": 1, "date": "2026-09-01" }
```

`dayNumber`와 `date`는 일정의 시작일·종료일 범위와 일치해야 한다.

### POST /api/itineraries/{id}/days/{dayId}/items

요청:
```json
{
  "placeId": 10,
  "visitOrder": 1,
  "startTime": "10:00",
  "endTime": "11:30",
  "memo": "예약 필요",
  "travelMinutesBefore": 20,
  "distanceKmBefore": 5.4,
  "estimatedCost": 15000
}
```

같은 일자 안에서 `visitOrder`는 중복될 수 없고, 시작·종료 시간이 모두 있으면 종료 시간이 시작 시간보다 늦어야 한다. 이동시간·거리·예상비용은 선택값이며 0 이상이어야 한다. 자동 생성 일정은 세 값을 구조화해 저장하므로 이후 상세 조회에서도 그대로 반환한다.

## 3. 장소 (place)

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/places` | 목록/필터 조회 | 불필요 |
| GET | `/api/places/{id}` | 단건 조회 | 불필요 |
| POST | `/api/places` | 장소 등록 | 필요 (권한 범위 결정 대기 — 정책 문서 3절) |
| PUT | `/api/places/{id}` | 장소 수정 | 필요 (등록과 동일 정책) |
| DELETE | `/api/places/{id}` | 장소 삭제 | 필요 (등록과 동일 정책) |

### GET /api/places?region=&category=

쿼리 파라미터는 모두 선택값. `category`는 DB설계 문서 2.6절의 허용 값만 사용(`FOOD`, `CAFE`, `SIGHT`, `ACCOMMODATION`).

성공 응답 (`data`): `Place[]`
```json
[
  {
    "id": 10,
    "name": "해운대 해수욕장",
    "category": "SIGHT",
    "region": "부산",
    "address": "...",
    "latitude": 35.1587,
    "longitude": 129.1604,
    "priceRange": null,
    "openHours": "09:00~20:00",
    "openingTime": "09:00",
    "closingTime": "20:00",
    "estimatedCost": 0,
    "recommendedVisitMinutes": 90
  }
]
```

### POST /api/places

요청 body는 위 응답 형태에서 `id` 제외. 좌표와 영업시간은 각각 한 쌍으로 입력한다. `estimatedCost`는 0 이상, `recommendedVisitMinutes`는 30~480분이다. **등록 권한(관리자 전용 vs 사용자 개방)이 정책 문서 3절에 결정 대기로 남아 있다. 현재 구현은 "인증된 사용자면 누구나 가능"(ADMIN 제한 없음)으로, 결정 대기 항목을 임의로 확정하지 않기 위한 상태다.** 정책이 확정되면 이 문서와 `SecurityConfig`/`PlaceController`를 함께 갱신한다.

## 4. 여행 계획 생성 (planning)

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/travel-plans/generate` | 여행 조건으로 날짜별 일정 초안을 생성하고 저장 | 필요 |

요청:
```json
{
  "title": "부산 2박 3일",
  "city": "부산",
  "startDate": "2026-10-01",
  "endDate": "2026-10-03",
  "budget": 500000,
  "preferredCategories": ["SIGHT", "FOOD"],
  "companionType": "FRIENDS",
  "travelPace": "BALANCED",
  "dailyStartTime": "09:00",
  "dailyEndTime": "18:00",
  "requiredPlaceIds": [10],
  "excludedPlaceIds": [20],
  "startLatitude": 35.1587,
  "startLongitude": 129.1604
}
```

- `companionType`: `SOLO`, `COUPLE`, `FAMILY`, `FRIENDS`
- `travelPace`: `RELAXED`(하루 최대 3곳), `BALANCED`(4곳), `ACTIVE`(5곳)
- 동행 형태별 하루 상한: `FAMILY` 3곳, `COUPLE` 4곳, `SOLO`·`FRIENDS` 5곳. 여행 속도 상한과 비교해 더 작은 값을 적용한다.
- 하루 시간의 기본값은 `09:00`~`18:00`이다. 체류시간 미입력 시 카테고리 기본값(`FOOD`·`SIGHT` 90분, `CAFE`·`ACCOMMODATION` 60분)을 사용한다.
- 출발 좌표는 선택값이지만 위도·경도를 함께 입력해야 한다. 입력 시 첫 장소까지의 이동량도 계산한다.
- 필수 장소 → 선호 카테고리 → 실제 방문 가능 시작 시각 → 거리 순으로 후보를 선택한다.
- 좌표가 있으면 Haversine 직선거리와 평균 시속 20km로 이동시간을 추정한다. 좌표가 없는 장소 사이 이동시간은 30분으로 계산한다.
- 선택 장소가 예산을 초과하면 제외한다. 필수 장소는 예산보다 우선하며, 초과 사실을 `warnings`로 반환한다.
- 영업시간 안에 배치할 수 없는 필수 장소가 있으면 전체 생성을 거부한다.
- 여행 기간은 최대 14일이다. 생성·일자·방문 항목 저장은 하나의 트랜잭션으로 처리한다.
- `ROUTE_AWARE_V2`는 실제 도로망·교통상황이 아닌 직선거리 추정치를 사용한다. 요일별 영업시간은 아직 지원하지 않는다.

성공 응답 (`data`):
```json
{
  "generator": "ROUTE_AWARE_V2",
  "selectedPlaceCount": 6,
  "availableSlotCount": 12,
  "estimatedDistanceKm": 12.35,
  "estimatedTravelMinutes": 48,
  "estimatedCost": 85000,
  "warnings": ["이동 시간은 직선거리와 평균 시속 20km를 기준으로 한 추정치입니다."],
  "itinerary": { "id": 1, "title": "부산 2박 3일", "days": [] }
}
```

실패:
- `G001`(422): 해당 도시에 후보 장소 없음
- `G002`(400): 필수 장소가 여행 도시와 다름
- `G003`(400): 필수 장소 수가 배치 가능 수를 초과
- `G004`(422): 필수 장소를 영업시간과 일정 시간 안에 배치할 수 없음

## 5. 공통 DTO 참고

| DTO | 필드 |
|---|---|
| `ItinerarySummary` | id, title, city, startDate, endDate, status, myRole |
| `ItineraryDetail` | ItinerarySummary + budget, shareToken(nullable), days[](ItineraryDay + items[]), participants[] |
| `ItineraryDay` | id, dayNumber, date |
| `ItineraryItem` | id, placeId, placeName, visitOrder, startTime, endTime, memo, travelMinutesBefore, distanceKmBefore, estimatedCost |
| `Participant` | id, memberId, nickname, participantRole |
| `Place` | id, name, category, region, address, latitude, longitude, priceRange, openHours, openingTime, closingTime, estimatedCost, recommendedVisitMinutes |

## 6. 이 문서에서 결정 대기로 남긴 항목

- [x] ~~로그인 응답 바디 스펙~~ → JWT로 확정, 1절 참고.
- [ ] 공유 링크로 EDITOR 권한 부여 가능 여부 (현재 구현: 공유 링크는 항상 VIEWER)
- [ ] 장소 등록/수정/삭제 권한 범위(관리자 전용 vs 사용자 개방) — 현재 구현: 인증된 사용자면 누구나 가능
- [ ] 목록 조회 페이지네이션 규격 (데이터 양 증가 시, 현재 구현: 전체 목록 반환)

위 항목은 `docs/domain/트래블메이커-정책정리본.md`, `docs/architecture/TravelPlanner-SA.md` 9절과 동일한 미결정 목록이다. 구현 중 이 항목에 부딪히면 임의로 정하지 말고 확인 후 이 문서를 갱신한다.
