# Context Router

> 작업을 시작하기 전에, 작업 유형에 따라 아래 순서로 문서를 읽는다. 문서를 안 읽고 코드부터 짜지 않는다.

## 1. 새 도메인 CRUD API를 추가할 때

1. `docs/product/TravelPlanner-PRD.md` — 이 기능이 몇 단계(1/2/3단계) 범위인지 확인. 범위 밖이면 작업을 시작하지 않고 사람에게 확인한다.
2. `docs/architecture/TravelPlanner-SA.md` — 패키지 위치, 레이어 규칙, 공통 골격(ApiResponse/ErrorCode/BaseTimeEntity) 사용법.
3. `docs/architecture/TravelPlanner-API명세.md` — 경로, 요청/응답 필드가 이미 정의되어 있는지 확인. 없거나 다르면 코드보다 이 문서를 먼저 갱신.
4. `docs/architecture/TravelPlanner-DB설계.md` — 관련 테이블이 이미 정의되어 있는지 확인. 없거나 다르면 코드보다 이 문서를 먼저 갱신.
5. `docs/domain/트래블메이커-정책정리본.md` — 권한/검증 규칙에 "결정 대기"로 표시된 항목이 있는지 확인. 있으면 임의로 정하지 말고 사람에게 확인한다.
6. 구현 후 `docs/ai/completion-checklist.md`로 자체 점검.

## 2. 기존 코드를 수정/리팩터링할 때

1. `docs/architecture/TravelPlanner-SA.md` 4절(레이어 규칙)과 비교해 현재 코드가 규칙을 지키고 있는지 확인.
2. 규칙과 다르게 짜여 있어도, PRD/SA/DB설계/정책 문서를 먼저 갱신하지 않고 규칙만 임의로 바꾸지 않는다 — 의도된 예외인지 사람에게 확인.

## 3. 질문의 답이 문서마다 다르게 보일 때

`docs/ai/rule-source-map.md`의 우선순위 표를 따른다. 그래도 애매하면 사람에게 묻는다.

## 4. "이 프로젝트에서 못 보던 개념"을 구현해야 할 때 (예: 새 외부 API, 새 인프라)

- 기존 4개 문서(PRD/SA/DB설계/정책) 어디에도 근거가 없으면 구현하지 않는다.
- 특히 `docs/architecture/TravelPlanner-SA.md` 9절과 `docs/architecture/TravelPlanner-DB설계.md` 4절의 "결정 대기" 목록에 있는 항목(인증 방식, 검색엔진 종류, 분산 락 도구 등)은 절대 임의로 선택하지 않는다.

## 5. 매 작업 공통

- 작업 시작 전: 위 라우팅을 따라 관련 문서를 읽는다.
- 작업 중: `docs/ai/implementation-guardrails.md`의 제약을 지킨다.
- 작업 종료 전: `docs/ai/completion-checklist.md`, `docs/ai/remove-ai-slop.md`로 자체 점검한다.
- 문서와 다르게 구현해야만 하는 상황이면, 코드를 먼저 짜지 말고 문서 변경을 먼저 제안한다.
