# TravelPlanner 3단계 완료 보고

- 완료일: 2026-10-20
- 구현 브랜치: `codex/google-places-integration-v3`
- 외부 공급자: Google Places API (New)

## 완료 범위

### 외부 장소 검색·동기화

- `PlaceProvider` 인터페이스와 Google Places 구현체
- Text Search로 장소 ID 검색 후 Place Details 조회
- FieldMask로 필요한 필드만 요청
- Google 유형을 내부 `FOOD`, `CAFE`, `SIGHT`, `ACCOMMODATION`으로 변환
- `source + externalId` 기준 중복 방지와 갱신
- 외부 ID, 출처, 마지막 동기화 시각 저장
- 인증된 사용자의 `POST /api/places/import` 제공
- 저장된 도시 장소가 없을 때 여행 계획 생성 과정에서 자동 적재

### 영업시간

- 요일별 여러 영업 구간 저장
- 자정 이후 종료 구간 저장
- 여행 날짜의 요일에 맞춰 방문 가능 시각 계산
- 전날 시작해 다음 날 끝나는 구간의 새벽 방문 처리
- 기존 단일 시작·종료 시각과 하위 호환

### 장애·설정 처리

- API 키와 활성화 여부를 환경변수로 분리
- 연결·요청 타임아웃 설정
- 429, 5xx, 연결 오류 지수 백오프 재시도
- 키가 없을 때 기존 DB 장소로 일정 생성 유지
- 명시적 가져오기 요청의 설정 누락 `L003`, 외부 실패 `L004`

## 검증 범위

- 전체 자동화 테스트 30개 통과, 실패 0개, 오류 0개
- Text Search 재시도와 Place Details 응답 매핑
- 같은 외부 ID 재동기화 시 단일 행 유지
- 요일별 영업 시작 시각에 맞춘 일정 배치
- API 키가 없을 때 예측 가능한 오류 응답
- 기존 인증·장소·일정 생성 회귀 테스트

## 실행 전 필요한 외부 설정

Google Cloud 프로젝트에서 Places API (New)와 결제를 활성화하고 다음 환경변수를 설정한다.

```text
GOOGLE_PLACES_ENABLED=true
GOOGLE_PLACES_API_KEY=발급받은_키
```

실제 과금 계정과 API 키가 저장소에 제공되지 않았으므로 라이브 Google 호출은 실행하지 않았다. HTTP 계약 테스트는 로컬 모의 서버로 검증했다.

## 다음 단계

4단계에서 실제 도로망·교통수단별 이동시간 공급자를 선정하고 현재 Haversine 추정 계산의 대체 경로로 연결한다.
