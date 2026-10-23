# TravelPlanner 5단계 완료 보고

- 완료일: 2026-10-23
- 구현 브랜치: `codex/ai-itinerary-revision-v5`
- AI 공급자: OpenAI Responses API
- 기본 모델: `gpt-6-luna`

## 완료 범위

### 자연어 일정 수정

- `POST /api/itineraries/{id}/ai-revisions` 추가
- 자연어 요청과 현재 일정, 같은 도시의 후보 장소를 AI 공급자에 전달
- OpenAI strict Structured Outputs로 `UPDATE`, `REMOVE` 명령만 수신
- 변경 요약, 공급자, 모델, 프롬프트 버전, 변경 전후 값과 최종 일정 반환
- 공유 토큰과 동행자 정보는 외부 AI 컨텍스트에서 제외

### 서버 검증과 트랜잭션

- `OWNER`, `EDITOR`만 AI 수정 가능하며 권한 검증 후 외부 호출
- AI가 반환한 일자, 방문 항목, 장소 ID를 서버 데이터로 재검증
- 여행 도시, 장소 영업시간, 같은 날 방문시간 겹침, 전체 예상 비용 검증
- 장소·시간 변경 시 오래된 이동시간과 거리 추정값 제거
- 모든 변경을 하나의 트랜잭션으로 적용하고 하나라도 실패하면 전체 롤백
- 설정 누락, 외부 실패, 잘못된 명령, 크기·명령 수 초과를 `G005`~`G008`로 구분

### 비용과 운영 제한

- 비용 효율적인 `gpt-6-luna`를 기본 모델로 사용
- 기본 최대 출력 1,200토큰, 명령 5개, 프롬프트 30,000자
- 연결·요청 타임아웃과 429·5xx 재시도
- 모델, 추론 수준, 토큰과 입력 제한을 환경변수로 분리
- 프롬프트 버전 `itinerary-revision-v1`을 응답과 AI 입력에 기록

## 검증 범위

- 전체 자동화 테스트 42개 통과, 실패 0개, 오류 0개
- Authorization, 모델, strict JSON Schema, 저장 비활성화 요청 계약
- 5xx 재시도와 Structured Output 응답 해석
- 정상 장소·시간 변경과 최종 일정 응답
- 예산 검증 실패 시 기존 일정 보존
- VIEWER 요청이 외부 AI 호출 전에 차단되는지 검증
- AI 공급자 미설정 오류 검증
- 기존 인증·장소·일정 생성·Google Places·Google Routes 회귀 테스트

## 실행 전 필요한 외부 설정

```text
OPENAI_AI_REVISION_ENABLED=true
OPENAI_API_KEY=발급받은_키
```

실제 API 키는 저장소에 포함하지 않았다. 라이브 호출 대신 로컬 HTTP 서버로 OpenAI 요청·응답 계약을 검증했다. [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses)와 [Responses API](https://developers.openai.com/api/reference/resources/responses/methods/create) 공식 문서를 기준으로 구현했다.

## 커밋 규칙 전환

5단계 고도화 시작과 함께 커밋 제목 규칙을 `<type>: <English imperative summary>` 형식으로 변경했다. `main`의 기존 한글 커밋 제목도 영어로 변환했으며 파일 내용과 병합 구조는 유지했다.

## 다음 단계

6단계에서 MySQL, Flyway, 환경별 비밀값, Docker와 CI 통합 테스트를 포함한 운영 배포 기반을 구성한다.
