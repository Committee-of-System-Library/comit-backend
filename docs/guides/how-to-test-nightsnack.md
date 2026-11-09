# 야식마차 INSERT 접수와 스케줄러 선정 검증

직접 서버를 실행해 확인하려면 [로컬 수동 테스트](./night-snack-manual-test.md)를 따른다.

기준 설계는 [야식마차 비동기 선정](../features/night-snack-async-selection.md)이다.
일반 정원은 전체 정원에서 예약분을 뺀 값이다. 일반 정원의 최대 절반은 즉시 확정되며,
나머지는 마감 시 ID 오름차순으로 선정된다. 일반 정원보다 많은 접수 행이 저장될 수 있다.

## 자동 검증

Docker를 실행하고 다음 명령을 사용한다. 테스트는 실제 MySQL 컨테이너를 생성한다.

```bash
./gradlew test --tests 'kr.ac.knu.comit.nightsnack.*' --tests 'kr.ac.knu.comit.api.NightSnackApiWebTest'
./gradlew generateApiDocs
```

구형 Testcontainers 클라이언트와 Docker 29의 API 버전이 충돌하는 환경에서는 다음처럼 실행한다.

```bash
JAVA_TOOL_OPTIONS=-Dapi.version=1.44 ./gradlew test --tests 'kr.ac.knu.comit.nightsnack.*'
```

주요 검증은 동시 접수 300건 중 100건 확정, 동일 회원 중복, 지연 커밋과 마감 경합,
롤백·타 행사로 생긴 ID 공백, 예약분 격리, 기간 오픈·마감과 정원 0명 처리다.

## API 확인

1. 관리자 생성 API로 신청 기간과 정원을 지정한다. 예약분은 설정 비율로 분리된다.
2. 기간 시작 시 스케줄러가 자동 오픈한다. 관리자 PATCH `/admin/night-snacks/{id}/open`도 사용할 수 있다.
3. 로그인한 학생이 POST `/night-snacks/{id}/applications`를 호출한다.
4. 아래처럼 PENDING이면 GET `/night-snacks/{id}/my-ticket`을 1초 간격으로 조회한다.
5. ACCEPTED이면 티켓을 표시하고, REJECTED이면 탈락을 안내한다. 두 경우 모두 폴링을 끝낸다.

```json
{
  "result": "SUCCESS",
  "data": {
    "applicationId": 42,
    "selectionStatus": "PENDING",
    "ticketToken": null
  }
}
```

selectionStatus는 당락이고 기존 status는 수령 상태다. 신청 HTTP 200은 접수 완료를 뜻한다.
일반 정원 100명에 300명이 접수하면 최종 성공은 100명, 탈락은 200명이어야 한다.
즉시 성공한 신청은 최종 선정 후에도 성공이어야 한다. ID 공백으로 즉시 성공이 50명보다 적어도
최종 성공은 실제 접수 행 기준으로 100명을 채워야 한다.

## 설정과 마이그레이션

- `NIGHT_SNACK_SCHEDULER_ENABLED=true`, `NIGHT_SNACK_SCHEDULER_DELAY_MS=1000`이 기본값이다.
- 전략 선택 설정은 제거했다. 자동 스케줄러를 끈 테스트에서는 LifecycleService.refresh를 직접 호출한다.
- V116은 기존 신청을 ACCEPTED로 이관하고 일반 remaining 컬럼을 제거한다.
- 이전 서버 바이너리는 remaining 컬럼에 의존하므로 마이그레이션과 새 서버·클라이언트 배포를 맞춘다.

## 검증 기록 (2026-11-08)

관련 도메인·서비스·웹 API·애플리케이션 컨텍스트 테스트 63개가 실패·스킵 없이 통과했다.
이 중 MySQL 8.0.36 통합 테스트는 8개이며, 마감 저장 실패 시 당락 갱신 롤백과 재실행 복구를 포함한다.
API 문서 생성과 전체 테스트 소스 컴파일도 확인했다.


## PR 분리 후 검증 (2026-11-09)

INSERT 전용 Writer를 `NightSnackInsertWriter`로 독립시킨 뒤 각 코드 단계에서 야식마차 회귀 테스트와
전체 테스트 소스 컴파일·API 문서 생성을 확인했다. 최종 단계에서는 실제 MySQL 통합 9개를 포함해
관련 테스트 64개가 실패·스킵 없이 통과했다. 원래 V115·V116 SQL 내용은 그대로 보존했다.
