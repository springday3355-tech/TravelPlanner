# TravelPlanner — API 명세 (기본 CRUD + 실제 장소 기반 여행 계획 초안 생성)

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
| POST | `/api/places/import` | Google Places 검색 결과 동기화 | 필요 |
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
    "recommendedVisitMinutes": 90,
    "source": "MANUAL",
    "externalId": null,
    "lastSyncedAt": null,
    "openingHours": [
      {
        "dayOfWeek": "MONDAY",
        "openingTime": "09:00",
        "closingTime": "20:00",
        "closesNextDay": false
      }
    ]
  }
]
```

### POST /api/places

요청 body는 위 응답 형태에서 `id`, `source`, `externalId`, `lastSyncedAt` 제외. 좌표와 기존 단일 영업시간은 각각 한 쌍으로 입력한다. `openingHours`에는 요일별 여러 영업 구간을 입력할 수 있고, 다음 날 종료되는 구간은 `closesNextDay=true`로 표시한다. `estimatedCost`는 0 이상, `recommendedVisitMinutes`는 30~480분이다. **등록 권한(관리자 전용 vs 사용자 개방)이 정책 문서 3절에 결정 대기로 남아 있다. 현재 구현은 "인증된 사용자면 누구나 가능"(ADMIN 제한 없음)으로, 결정 대기 항목을 임의로 확정하지 않기 위한 상태다.** 정책이 확정되면 이 문서와 `SecurityConfig`/`PlaceController`를 함께 갱신한다.

### POST /api/places/import

요청:

```json
{
  "query": "관광 명소",
  "region": "서울",
  "categories": ["SIGHT"],
  "limit": 10
}
```

- `limit`은 1~20이며 생략하면 10이다.
- Google Text Search로 ID를 찾고 Place Details의 이름·주소·좌표·유형·가격·요일별 영업시간을 가져온다.
- 같은 `source`와 `externalId`가 이미 있으면 새 장소를 만들지 않고 정보와 동기화 시각을 갱신한다.
- 성공 시 동기화된 `Place[]`와 HTTP 201을 반환한다.
- Google Places 설정이 없으면 `L003`(503), 외부 호출이 실패하면 제한된 재시도 뒤 `L004`(502)를 반환한다.

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
  "startLongitude": 129.1604,
  "transportMode": "TRANSIT"
}
```

- `companionType`: `SOLO`, `COUPLE`, `FAMILY`, `FRIENDS`
- `travelPace`: `RELAXED`(하루 최대 3곳), `BALANCED`(4곳), `ACTIVE`(5곳)
- `transportMode`: `TRANSIT`(기본값), `DRIVE`, `WALK`
- 동행 형태별 하루 상한: `FAMILY` 3곳, `COUPLE` 4곳, `SOLO`·`FRIENDS` 5곳. 여행 속도 상한과 비교해 더 작은 값을 적용한다.
- 하루 시간의 기본값은 `09:00`~`18:00`이다. 체류시간 미입력 시 카테고리 기본값(`FOOD`·`SIGHT` 90분, `CAFE`·`ACCOMMODATION` 60분)을 사용한다.
- 출발 좌표는 선택값이지만 위도·경도를 함께 입력해야 한다. 입력 시 첫 장소까지의 이동량도 계산한다.
- 필수 장소 → 선호 카테고리 → 실제 방문 가능 시작 시각 → 거리 순으로 후보를 선택한다.
- Google Routes가 활성화되어 있으면 선택한 교통수단의 도로망 이동시간 행렬을 사용한다. 비활성화·장애·비용 상한 초과·일부 경로 누락 시 Haversine 직선거리와 평균 시속 20km 추정으로 전환한다. 좌표가 없는 장소 사이 이동시간은 30분으로 계산한다.
- 선택 장소가 예산을 초과하면 제외한다. 필수 장소는 예산보다 우선하며, 초과 사실을 `warnings`로 반환한다.
- 영업시간 안에 배치할 수 없는 필수 장소가 있으면 전체 생성을 거부한다.
- 해당 도시에 저장된 장소가 없고 Google Places가 설정되어 있으면 선호 카테고리로 장소를 먼저 가져온다.
- 여행 기간은 최대 14일이다. 생성·일자·방문 항목 저장은 하나의 트랜잭션으로 처리한다.
- 외부 경로가 적용되면 `ROUTE_MATRIX_V4`, 대체 계산만 사용하면 `ROUTE_AWARE_V2`를 반환한다. 요일별 여러 영업 구간과 자정 이후 종료 구간을 여행 날짜에 맞춰 사용한다.

성공 응답 (`data`):
```json
{
  "generator": "ROUTE_MATRIX_V4",
  "selectedPlaceCount": 6,
  "availableSlotCount": 12,
  "estimatedDistanceKm": 12.35,
  "estimatedTravelMinutes": 48,
  "estimatedCost": 85000,
  "warnings": ["이동 시간과 거리는 Google Routes 도로망 기준입니다."],
  "itinerary": { "id": 1, "title": "부산 2박 3일", "days": [] }
}
```

실패:
- `G001`(422): 해당 도시에 후보 장소 없음
- `G002`(400): 필수 장소가 여행 도시와 다름
- `G003`(400): 필수 장소 수가 배치 가능 수를 초과
- `G004`(422): 필수 장소를 영업시간과 일정 시간 안에 배치할 수 없음

## 5. AI 일정 수정 (planning/ai)

### POST /api/itineraries/{id}/ai-revisions

인증된 `OWNER` 또는 `EDITOR`가 자연어로 기존 일정의 장소와 시간을 변경하거나 방문 항목을 제거한다.

요청:

```json
{
  "instruction": "비가 오면 첫날 야외 일정을 실내 장소로 바꿔줘"
}
```

- `instruction`은 필수이며 최대 1,000자다.
- OpenAI Responses API의 strict JSON Schema 출력만 변경 명령으로 해석한다.
- 한 요청의 변경 명령은 기본 최대 5개다.
- AI에는 일정 일자·방문 항목과 같은 도시의 후보 장소만 전달하며 공유 토큰과 동행자 정보는 전달하지 않는다.
- 서버는 AI 결과를 신뢰하지 않고 일자·방문 항목·장소 ID, 여행 도시, 영업시간, 시간 겹침, 예산을 다시 검증한다.
- 모든 명령은 하나의 트랜잭션으로 적용한다. 하나라도 실패하면 원래 일정을 보존한다.
- 장소나 시간이 바뀌면 기존 이동시간·거리 추정값은 무효화해 `null`로 저장한다.

성공 응답 (`data`):

```json
{
  "summary": "첫날 야외 공원을 실내 박물관으로 변경했습니다.",
  "provider": "OPENAI",
  "model": "gpt-6-luna",
  "promptVersion": "itinerary-revision-v1",
  "changes": [
    {
      "operation": "UPDATE",
      "itemId": 10,
      "beforePlaceId": 20,
      "afterPlaceId": 30,
      "beforeStartTime": "09:00:00",
      "afterStartTime": "10:00:00",
      "reason": "우천 대응"
    }
  ],
  "itinerary": { "id": 1, "days": [] }
}
```

실패:

- `P002`(403): 편집 권한 없음. 외부 AI 호출 전에 차단한다.
- `G005`(503): AI 공급자 설정 또는 API 키 없음
- `G006`(502): 외부 AI 호출 실패, 거부, 불완전하거나 해석 불가능한 응답
- `G007`(422): 제안된 명령이 권한 외 리소스, 영업시간, 겹침, 예산 검증에 실패
- `G008`(400): 프롬프트 크기, 명령 개수, 응답 길이 제한 초과

## 6. 공통 DTO 참고

| DTO | 필드 |
|---|---|
| `ItinerarySummary` | id, title, city, startDate, endDate, status, myRole |
| `ItineraryDetail` | ItinerarySummary + budget, shareToken(nullable), days[](ItineraryDay + items[]), participants[] |
| `ItineraryDay` | id, dayNumber, date |
| `ItineraryItem` | id, placeId, placeName, visitOrder, startTime, endTime, memo, travelMinutesBefore, distanceKmBefore, estimatedCost |
| `Participant` | id, memberId, nickname, participantRole |
| `Place` | id, name, category, region, address, latitude, longitude, priceRange, openHours, openingTime, closingTime, estimatedCost, recommendedVisitMinutes, source, externalId, lastSyncedAt, openingHours[] |

## 7. 이 문서에서 결정 대기로 남긴 항목

- [x] ~~로그인 응답 바디 스펙~~ → JWT로 확정, 1절 참고.
- [ ] 공유 링크로 EDITOR 권한 부여 가능 여부 (현재 구현: 공유 링크는 항상 VIEWER)
- [ ] 장소 등록/수정/삭제 권한 범위(관리자 전용 vs 사용자 개방) — 현재 구현: 인증된 사용자면 누구나 가능
- [ ] 목록 조회 페이지네이션 규격 (데이터 양 증가 시, 현재 구현: 전체 목록 반환)

위 항목은 `docs/domain/트래블메이커-정책정리본.md`, `docs/architecture/TravelPlanner-SA.md` 9절과 동일한 미결정 목록이다. 구현 중 이 항목에 부딪히면 임의로 정하지 말고 확인 후 이 문서를 갱신한다.
