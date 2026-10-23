# TravelPlanner 4단계 완료 보고

- 완료일: 2026-10-23
- 구현 브랜치: `codex/google-routes-integration-v4`
- 외부 공급자: Google Routes API Compute Route Matrix

## 완료 범위

### 실제 이동 경로

- `RouteMatrixProvider` 계약과 Google Routes 구현체 추가
- `TRANSIT`, `DRIVE`, `WALK` 교통수단 입력 지원, 미입력 기본값은 `TRANSIT`
- 출발 좌표와 후보 장소, 후보 장소 상호 간 이동시간·거리 행렬 계산
- 외부 경로가 있으면 `ROUTE_MATRIX_V4`로 실제 이동시간을 방문 가능 여부와 후보 순서에 반영
- 실제 이동시간으로 하루 종료시각을 넘는 장소는 배치하지 않음

### 비용·장애 제어

- 대중교통은 Google 제한에 맞춰 요청당 최대 100요소로 분할
- 계획당 기본 120요소 상한을 적용해 과도한 유료 요청 차단
- 좌표·교통수단 조합별 메모리 캐시와 30분 기본 TTL 적용
- 429·5xx·연결 오류 재시도
- 비활성화, 비용 상한, 외부 실패, 일부 경로 누락 시 Haversine 추정으로 자동 전환
- 대체 계산 사유를 일정 응답의 `warnings`에 구분해 제공

## 검증 범위

- 전체 자동화 테스트 37개 통과, 실패 0개, 오류 0개
- Google Routes 인증 헤더, FieldMask, 교통수단 요청과 응답 변환
- 재시도와 대중교통 100요소 요청 분할
- 동일 행렬 캐시 재사용과 계획당 비용 상한
- 실제 이동시간으로 불가능한 필수 장소 배치 차단
- 외부 경로 실패 시 직선거리 추정으로 일정 생성 유지
- 기존 인증·장소·일정·Google Places 회귀 테스트

## 실행 전 필요한 외부 설정

Google Cloud 프로젝트에서 Routes API와 결제를 활성화하고 다음 환경변수를 설정한다.

```text
GOOGLE_ROUTES_ENABLED=true
GOOGLE_ROUTES_API_KEY=발급받은_키
```

Routes와 Places에 같은 키를 사용하면 `GOOGLE_ROUTES_API_KEY`를 생략할 수 있다. 실제 과금 계정과 API 키는 저장소에 포함하지 않았으므로 라이브 Google 호출 대신 로컬 HTTP 계약 테스트로 검증했다.

## 다음 단계

5단계에서 자연어 수정 요청을 검증 가능한 일정 변경 명령으로 변환하고, 변경 전후 차이와 실패 이유를 반환한다.
