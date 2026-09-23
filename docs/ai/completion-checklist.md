# Completion Checklist

> 작업을 "완료"라고 보고하기 전에 아래를 전부 확인한다. 하나라도 아니오면 완료로 보고하지 않는다.

## 구조/규칙

- [ ] 새 코드가 `docs/architecture/TravelPlanner-SA.md`의 패키지 구조와 레이어 규칙(4절)을 따르는가?
- [ ] 컨트롤러가 `ApiResponse<T>`로만 응답하는가?
- [ ] 서비스가 `BusinessException`만 던지는가? 새 실패 케이스는 `ErrorCode`에 추가했는가?
- [ ] 새 엔티티가 `BaseTimeEntity`를 상속하는가?
- [ ] `docs/architecture/TravelPlanner-DB설계.md`의 테이블 정의와 실제 엔티티 필드가 일치하는가? 다르면 문서를 함께 갱신했는가?
- [ ] `docs/architecture/TravelPlanner-API명세.md`의 경로/요청·응답 필드와 실제 구현이 일치하는가?

## 범위

- [ ] 이번 작업이 `docs/product/TravelPlanner-PRD.md`의 해당 단계(1/2/3단계) 범위 안에 있는가?
- [ ] `docs/domain/트래블메이커-정책정리본.md`의 "결정 대기" 항목을 임의로 채우지 않았는가?

## 테스트/빌드

- [ ] 기존 테스트 하네스(`ControllerTestSupport`/`IntegrationTestSupport`/`RepositoryTestSupport`)를 재사용했는가?
- [ ] 새 기능에 대한 테스트를 추가했는가? 그 테스트가 실제로 실패할 수 있는 assert를 포함하는가?
- [ ] `./gradlew test`를 실제로 실행했는가? 실행했다면 통과했는가? 실행하지 못했다면(환경 제약 등) 그 사실을 명시했는가?

## 문서 동기화

- [ ] 코드 구조/DB/정책이 바뀌었다면 관련 문서(SA/DB설계/정책/PRD)를 함께 갱신했는가?
- [ ] 경량본(`docs/lightweight/`)이 전체본과 어긋나게 되지 않았는가?

## 보고

- [ ] 실제로 한 일과 못한 일(환경 제약, 미검증 항목 등)을 있는 그대로 보고했는가?
