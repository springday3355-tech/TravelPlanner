# TravelPlanner 운영 런북

- 기준일: 2026-11-01
- 대상: MySQL 8.4 + Spring Boot `prod` 프로파일 + Docker Compose

## 1. 최초 실행

1. `.env.example`을 `.env`로 복사한다.
2. `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`에 서로 다른 충분히 긴 값을 설정한다.
3. 필요한 외부 API만 활성화하고 각 API 키를 설정한다.
4. 컨테이너를 빌드하고 실행한다.

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
```

MySQL 상태 검사가 통과한 뒤 애플리케이션이 시작된다. Flyway가 `db/migration`의 미적용 버전을 실행하고 Hibernate가 엔티티와 스키마를 검증한다. `http://localhost:8080/api/health`가 성공 응답을 반환해야 배포가 완료된 것이다.

## 2. 배포와 마이그레이션

- 적용된 마이그레이션 파일은 수정하거나 삭제하지 않는다.
- 스키마 변경은 `V2__...sql`처럼 다음 버전 파일로 추가한다.
- 배포 전에 CI의 `Gradle test`와 `MySQL migration` 작업이 모두 성공해야 한다.
- 마이그레이션 실패 시 애플리케이션은 기동하지 않는다. 로그에서 실패 버전을 확인하고 SQL을 수정한 새 배포본을 만든다.
- 운영 DB에 `ddl-auto=update`를 사용하거나 수동 DDL을 먼저 적용하지 않는다.

상태와 최근 로그 확인:

```powershell
docker compose ps
docker compose logs --tail 200 app
docker compose logs --tail 200 mysql
```

## 3. 백업

배포 전과 정기 백업 시 일관된 논리 백업을 생성한다. `backups` 폴더는 저장소 밖 또는 접근이 제한된 저장소에 보관하고 암호화와 보존 기간을 조직 정책에 맞춘다.

```powershell
New-Item -ItemType Directory -Force backups
docker compose exec -T mysql sh -c 'exec mysqldump --single-transaction --routines --triggers -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > backups/travelplanner.sql
```

백업 파일의 크기와 종료 코드를 확인하고, 정기적으로 격리된 MySQL에 복원해 실제 사용 가능 여부를 검사한다.

## 4. 복구

복구 전에 애플리케이션 쓰기를 중단하고 대상 DB의 별도 백업을 만든다. 복구 파일을 MySQL 컨테이너로 복사한 뒤 가져온다.

```powershell
docker compose stop app
docker compose cp backups/travelplanner.sql mysql:/tmp/travelplanner-restore.sql
docker compose exec mysql sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < /tmp/travelplanner-restore.sql'
docker compose exec mysql rm -f /tmp/travelplanner-restore.sql
docker compose start app
```

복구 후 `/api/health`, 회원 인증, 일정 조회를 확인하고 Flyway 로그에 검증 오류가 없는지 확인한다.

## 5. 비밀값과 로그 정책

- `.env`와 실제 키는 Git에 커밋하지 않는다. 배포 플랫폼에서는 Secret Manager 또는 보호된 환경변수를 사용한다.
- 운영 로그에 요청 본문, `Authorization` 헤더, JWT, DB 비밀번호, 외부 API 키를 기록하지 않는다.
- 운영 기본 로그는 `INFO`이며 Hibernate SQL과 바인딩 값 로그를 끈다.
- 장애 분석에 필요한 경우에도 민감값을 가린 뒤 제한된 기간만 상세 로그를 활성화한다.
- 접근 로그와 애플리케이션 로그는 배포 플랫폼에서 순환, 보존 기간, 접근 권한을 설정한다.

## 6. 종료와 데이터 보존

```powershell
docker compose down
```

이 명령은 컨테이너와 네트워크만 제거하고 `mysql-data` 볼륨은 유지한다. 볼륨 삭제는 백업과 복구 검증이 끝난 뒤 명시적인 데이터 폐기 절차로만 수행한다.
