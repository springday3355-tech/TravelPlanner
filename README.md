# TravelPlanner

여행 조건을 바탕으로 일정을 생성하고, 동행자와 공유·편집하는 여행 플래너의 백엔드입니다.

## 현재 상태

- 0단계 안정화 완료: Spring Boot 4.1 호환, 핵심 데이터 검증, 테스트 기반 정리
- 1단계 CRUD 완료: 회원/JWT 인증, 일정·일자·방문 항목·동행자, 장소
- 1단계 완료: 여행 조건 입력 → 장소 후보 선정 → 날짜별 초안 생성 → 일정 저장
- 2단계 완료: 좌표 기반 동선, 구조화된 영업시간·예상 비용·체류시간을 반영하고 방문별 추정값을 저장하는 `ROUTE_AWARE_V2`
- 3단계 완료: Google Places Text Search·Place Details 연동, 외부 장소 중복 동기화, 요일별·자정 이후 영업시간 반영
- 4단계 완료: Google Routes 이동시간 행렬, 대중교통·자동차·도보 선택, 캐시·비용 상한·장애 대체 적용
- 이후 목표: AI 대화형 수정

## 기술 스택

- Java 21
- Spring Boot 4.1
- Spring Data JPA
- Spring Security + JWT
- H2 (로컬·테스트)
- Gradle Wrapper

## 실행

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

기본 실행 환경은 H2를 사용합니다. 애플리케이션은 `http://localhost:8080`에서 실행되며 상태 확인 API는 `GET /api/health`입니다.

## 테스트

Windows:

```powershell
.\gradlew.bat test
```

macOS/Linux:

```bash
./gradlew test
```

## 주요 API

- `POST /api/auth/signup`, `POST /api/auth/login`
- `GET /api/members/me`
- `POST /api/travel-plans/generate`: 여행 조건으로 일정 초안 생성·저장
- `/api/itineraries`: 일정과 동행자, 일자, 방문 항목 관리
- `/api/places`: 장소 등록, 조회, 수정, 삭제
- `POST /api/places/import`: Google Places의 실제 장소 검색·동기화
- `GET /api/itineraries/shared/{shareToken}`: 공유 일정 조회

세부 계약은 [`docs/architecture/TravelPlanner-API명세.md`](docs/architecture/TravelPlanner-API명세.md)를 참고하세요.

## 설정

`src/main/resources/application.properties`의 JWT 비밀키는 로컬 개발용입니다. 배포 환경에서는 외부 비밀값으로 교체해야 합니다. 운영 데이터베이스와 배포 구성은 여행 계획의 핵심 흐름을 검증한 뒤 확정합니다.

Google Places를 사용하려면 `.env.example`을 참고해 `GOOGLE_PLACES_ENABLED=true`와 `GOOGLE_PLACES_API_KEY`를 환경변수로 설정합니다. 키가 없으면 기존 DB 장소만 사용하며 외부 가져오기 API는 `L003`을 반환합니다.

실제 이동 경로를 사용하려면 Google Cloud에서 Routes API를 활성화하고 `GOOGLE_ROUTES_ENABLED=true`를 설정합니다. `GOOGLE_ROUTES_API_KEY`를 생략하면 Places 키를 함께 사용합니다. 설정 누락, 호출 실패, 계획당 요소 상한 초과 시에는 기존 직선거리 추정으로 자동 전환합니다.

## 문서

- 제품 요구사항: [`docs/product/TravelPlanner-PRD.md`](docs/product/TravelPlanner-PRD.md)
- 시스템 구조: [`docs/architecture/TravelPlanner-SA.md`](docs/architecture/TravelPlanner-SA.md)
- API 명세: [`docs/architecture/TravelPlanner-API명세.md`](docs/architecture/TravelPlanner-API명세.md)
- DB 설계: [`docs/architecture/TravelPlanner-DB설계.md`](docs/architecture/TravelPlanner-DB설계.md)
- 비즈니스 정책: [`docs/domain/트래블메이커-정책정리본.md`](docs/domain/트래블메이커-정책정리본.md)
