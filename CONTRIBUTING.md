# 기여 규칙

## 커밋 메시지

고도화 단계부터 이 저장소의 커밋 제목은 영어 Conventional Commits 형식을 사용합니다.

```text
<type>: <English imperative summary>
```

허용 유형:

| type | 사용 기준 |
|---|---|
| `feat` | 사용자 기능이나 API 추가·확장 |
| `fix` | 오류 수정 |
| `refactor` | 동작 변화 없는 구조 개선 |
| `test` | 테스트만 추가·수정 |
| `docs` | 문서만 변경 |
| `chore` | 빌드, 저장소, 개발 환경 설정 |

제목은 영어 명령형으로 간결하게 작성합니다. 기능 구현과 해당 기능을 검증하는 테스트는 같은 커밋에 포함하고, 독립적으로 되돌릴 수 있는 설정과 문서는 별도 커밋으로 분리합니다.

예시:

```text
feat: persist estimated travel time in generated itineraries
fix: validate required place opening hours
docs: document stage 2 completion
```

저장소의 `.gitmessage`가 커밋 편집기의 기본 안내문으로 사용됩니다. 적용 명령은 다음과 같습니다.

```bash
git config commit.template .gitmessage
```

## 커밋 분리 기준

1. 설정 변경
2. 기능과 해당 테스트
3. 문서 동기화

각 커밋은 단독으로 이해하고 되돌릴 수 있어야 합니다.
