# Rule Source Map

> 질문/작업 유형별로 "어느 문서가 정답인지"를 정의한다. 두 문서가 충돌하면 이 표의 우선순위를 따른다.
> 이 표에 없는 유형의 질문이 반복되면, 새 행을 추가하고 해당 문서를 갱신한다.

| 질문/작업 유형 | 우선 참고 문서 | 비고 |
|---|---|---|
| 패키지 구조, 레이어 규칙, 공통 골격 사용법 | `docs/architecture/TravelPlanner-SA.md` | 급하면 `docs/lightweight/TravelPlanner-SA-경량본.md` |
| 무엇을 만들어야 하는지(기능 범위, 우선순위) | `docs/product/TravelPlanner-PRD.md` | 1단계/2단계/3단계 범위 확인 |
| 테이블/컬럼/관계 | `docs/architecture/TravelPlanner-DB설계.md` | 엔티티 필드 추가 전 반드시 확인 |
| 엔드포인트 경로/요청·응답 필드 | `docs/architecture/TravelPlanner-API명세.md` | 경로/DTO 필드명을 임의로 바꾸지 않는다 |
| 비즈니스 규칙(권한, 정책, 예외 케이스) | `docs/domain/트래블메이커-정책정리본.md` | "결정 대기" 항목은 임의 구현 금지 |
| 에러 코드/실패 응답 포맷 | `docs/architecture/TravelPlanner-SA.md` 5.2절 + `docs/domain/트래블메이커-정책정리본.md` 4절 | |
| 작업 시작 전 무엇을 읽어야 하는지 | `docs/ai/context-router.md` | |
| 흔한 실수 재발 방지 | `docs/ai/agent-mistakes.md` | |
| 작업을 끝냈다고 선언하기 전 확인 | `docs/ai/completion-checklist.md` | |
| 코드 품질/과잉 구현 방지 | `docs/ai/remove-ai-slop.md` | |
| 에이전트가 임의로 결정하면 안 되는 것 | `docs/ai/implementation-guardrails.md` + 각 문서의 "결정 대기" 절 | |

## 충돌 시 우선순위

1. 사람이 채팅으로 직접 지시한 내용 (세션 한정, 문서화되지 않았다면 임시 지시로 취급)
2. `docs/domain/트래블메이커-정책정리본.md` (비즈니스 규칙)
3. `docs/architecture/TravelPlanner-SA.md` / `TravelPlanner-DB설계.md` (구조/기술 규칙)
4. `docs/product/TravelPlanner-PRD.md` (범위)

문서 간 내용이 실제로 어긋나 있으면 코드를 짜기 전에 먼저 문서 불일치를 사람에게 알린다.
