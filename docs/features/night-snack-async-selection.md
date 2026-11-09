# 야식마차 INSERT 접수와 스케줄러 확정 설계

> 상태: 구현. 일반 정원 기준 최대 50%를 ID 구간으로 즉시 확정하고, 후반부는 ID 순 선착순으로 선정한다.
> 정원의 정확히 절반이 아니라 최대 절반을 즉시 확정한다. 빈 ID 때문에 부족한 인원은 마감 시 보충한다.

## 1. 목표와 범위

신청 요청에서는 신청 행을 INSERT하고 빠르게 응답한다. 정원의 앞선 50%는 즉시 성공을 안내하고,
나머지 신청은 결과 대기로 접수한다. 스케줄러가 접수 건수를 조회해 정원 도달 시 신청을 닫고,
결과 대기 신청의 성공 여부를 저장한다. 사용자는 내 신청 조회로 최종 결과를 확인한다.

후반부도 선착순으로 선정한다.
결과 대기는 취소자 발생 시 승계되는 예비 대기열과 다르다. 이번 설계에 예비 승계 기능은 포함하지 않는다.

기존 예약분이 있으므로 일반 정원 `C = capacity - reservedCapacity`에 50%를 적용한다.
즉시 확정 목표는 `H = floor(C / 2)`, 나머지 확정 정원은 `C - H`다. 예약분은 별도 유지한다.
예를 들어 일반 정원 100명이면 최대 50명을 즉시 확정하고, 나머지 자리는 스케줄러가 선정한다.
결과 대기 접수자는 50명을 넘을 수 있으며, 이 중 정원에 들지 못한 사람은 최종 탈락한다.

## 2. 동시성에서 반드시 구분할 것

### COUNT는 마감 판단에 사용한다

일반 SELECT COUNT는 다른 트랜잭션의 미커밋 INSERT를 보지 못한다.
따라서 각 신청 요청에서 `COUNT < H`를 확인한 뒤 즉시 확정하면 동시 요청이 같은 자리를 차지할 수 있다.
COUNT는 스케줄러의 마감 시작 조건에 사용하고, 요청별 확정 순번을 보장하는 수단으로 사용하지 않는다.

또한 주기적 마감 방식에서는 접수 행 수가 정원을 넘을 수 있다.
보장해야 하는 것은 `최종 성공 인원 <= C`이며, `접수 행 수 <= C`는 보장하지 않는다.

### 최대 50%의 즉시 확정 구간

요청별 카운터 UPDATE를 없애기 위해 ID 하한을 사용한다. 정확히 50%를 즉시 확정하는 보장은 하지 않는다.
즉시 확정 구간은 `idBase <= application.id < idBase + H`다.
단, OPEN 전에 기준점이 저장되고 모든 일반 신청 ID가 기준점 이상이어야 한다.
AUTO_INCREMENT에는 롤백·중복 실패로 빈 번호가 생기고 다른 행사·예약분도 같은 ID를 소비하므로,
`id - idBase + 1`을 실제 신청 인원수로 해석해서는 안 된다.
구간 밖 신청을 전부 PENDING으로 남겨 두면 빈 번호 때문에 부족해진 인원을 나중에 보충할 수 있다.

일반 신청은 `selection_status=PENDING`으로 INSERT하고,
즉시 확정 ID 구간의 신청은 엔티티 조회 시 ACCEPTED로 계산한다. 불변 ID 구간이므로 성공을 철회하지 않는다.
추가 UPDATE 없이 신청에 응답하며, 스케줄러가 마감 시 최종 상태를 DB에 저장한다.

## 3. 신청 상태와 수령 상태

현재 `NightSnackApplicationStatus.PENDING`은 수령 대기,
`CONFIRMED`는 QR 수령 완료를 뜻한다. 이 값을 신청 당락으로 재사용하지 않는다.
별도 `selectionStatus`로 당락을 제공한다.

| selectionStatus | 의미 | 화면 |
|---|---|---|
| PENDING | 접수 완료, 선정 전 | 신청 접수 완료 · 결과 확인 중 |
| ACCEPTED | 수령 자격 확정 | 신청 성공 · 티켓 사용 가능 |
| REJECTED | 정원 밖 최종 탈락 | 신청 마감 · 선정되지 않음 |

선정 상태는 `PENDING -> ACCEPTED / REJECTED`로만 전이한다.
이미 ACCEPTED인 신청은 재실행해도 REJECTED로 바꾸지 않는다.
QR 수령은 `selectionStatus = ACCEPTED`이고 수령 대기 상태인 신청에만 허용한다.
예약분은 등록 성공 시 ACCEPTED로 저장한다. 기존 신청 데이터는 V116에서 ACCEPTED로 이관한다.

## 4. 요청과 스케줄러 흐름

### 신청 요청

1. 인증, 납부 여부, 기간을 검증한다. 같은 행사·회원 및 학번의 중복은 기존 DB 유니크 제약으로 막는다.
2. 쓰기 트랜잭션에서 최신 행사 상태를 확인하고 접수한다. 안전한 마감 경계는 아래 규칙을 따른다.
3. 일반 신청을 PENDING으로 INSERT한다. 불변 ID 구간 내 신청은 응답과 조회에서 ACCEPTED로 계산한다.
4. 커밋 성공 후 응답한다. PENDING 응답에는 확정 순번과 확정 잔여 수량을 표시하지 않는다.
5. 응답 유실 후 재조회하면 기존 신청을 확인할 수 있어야 한다.

### 스케줄러

1. 기본 1초 간격으로 시작 시각에 도달한 SCHEDULED·OPEN 행사를 조회한다.
2. 행사별 새 READ COMMITTED 트랜잭션에서 배타 잠금을 얻고 최신 상태를 확인한다.
3. SCHEDULED는 ID 하한을 저장하고 OPEN으로 전환한다.
4. 진행 중인 접수의 커밋·롤백이 끝난 상태에서 COUNT한다. `일반 접수 건수 >= C` 또는 기간 만료 시 선정한다.
5. 일반 신청을 `id ASC`로 정렬해 앞선 `min(C, 접수 건수)`건을 ACCEPTED,
   나머지를 REJECTED로 일괄 UPDATE하고 행사를 CLOSED로 변경한다.
6. 결과 저장과 CLOSED 변경은 같은 트랜잭션으로 커밋한다. 실패하면 전체 롤백하고 다음 실행에서 재시도한다.

선착순의 기준은 DB 신청 ID 배정 순서로 정의한다. HTTP 도착 시각이나 커밋 완료 순서와 같다고 보장하지 않는다.
신청 마감 시각까지 정원이 차지 않아도 접수를 닫고 접수된 유효 신청을 확정한다.
다중 서버의 스케줄러는 같은 행사 잠금과 상태 재확인으로 중복 처리를 방지한다.

### 마감과 진행 중 신청 사이의 경계

COUNT를 확인한 즉시 결과를 공개하면 더 작은 ID를 받은 미커밋 신청이 나중에 들어올 수 있다.
이를 막기 위해 신청 트랜잭션의 행사 행 `SELECT ... FOR SHARE`와
마감 트랜잭션의 동일 행사 행 `SELECT ... FOR UPDATE`를 사용한다.

- 신청은 공유 잠금으로 최신 OPEN·기간을 확인하고 INSERT 후 커밋까지 잠금을 유지한다.
- 마감은 배타 잠금으로 기존 접수 트랜잭션 종료를 기다린다. 이후 진입하는 신청은 CLOSED 확인 후 거절한다.
- 최종 대상 조회는 배타 잠금 획득 뒤 최신 데이터를 읽도록 한다. 이전 COUNT 스냅샷을 재사용하지 않는다.
- 마감 트랜잭션은 처음부터 배타 잠금을 사용한다. 공유 잠금을 배타 잠금으로 승격하는 흐름은 피한다.
- 모든 일반 접수 경로가 같은 규칙을 지켜야 한다.

이는 신청끼리 공유 가능한 잠금을 추가하는 안이며, 요청 처리가 SQL 한 번의 INSERT만으로 끝나는 안은 아니다.
행사 행의 다른 UPDATE와는 경합할 수 있다. 임의로 몇 초 기다리는 것으로 미커밋 신청이 모두 끝났다고 가정하지 않는다.
지연된 작은 ID 신청과 마감이 경합하는 경우를 MySQL 통합 테스트에서 검증한다.

## 5. COUNT와 조회 계약

마감 전 집계 예시는 다음과 같다. 예약분과 일반분을 섞지 않는다.

```sql
SELECT COUNT(*)
FROM night_snack_application
WHERE night_snack_id = :nightSnackId
  AND source = 'GENERAL';
```

집계에는 일반 접수의 ACCEPTED와 PENDING을 모두 포함한다.
현재 `countByNightSnackId`는 예약분까지 포함하므로 그대로 사용하지 않는다.
기존 변경에 포함된 `(night_snack_id, source, id)` 인덱스의 활용 여부는 실행 계획으로 확인한다.

내 신청 조회 응답에는 `applicationId`, `selectionStatus`, 수령 상태를 포함한다.
PENDING 사용자는 해당 조회를 폴링하고, ACCEPTED 또는 REJECTED가 되면 폴링을 종료한다.
티켓 토큰이 존재하더라도 PENDING·REJECTED는 수령 자격이 없다. 서버가 스캔 시 다시 검증한다.
잔여 수량은 집계 시점의 참고 값으로 안내하며, 확정 성공을 보장하는 값으로 사용하지 않는다.

## 6. 정리한 기존 구현

기존 전략 인터페이스·time-based·append-only 구현과 ApplySnapshot을 제거했다. 단일 ReservationWriter가 접수하고 LifecycleService가 오픈·마감·선정한다.

- 요청별 일반 정원 감소 UPDATE와 정원 밖 INSERT 롤백을 제거했다.
- `ApplyResponse`의 sequence·remaining을 제거하고 applicationId·selectionStatus·ticketToken을 제공한다.
- 일반 잔여 수량은 실제 접수 건수로 계산한다. V116에서 사용하지 않는 remaining 컬럼을 제거한다.
- 신청 엔티티·DTO·QR 수령 검증은 선정 상태와 수령 상태를 구분한다.
- V115는 기존 마이그레이션 이력을 보존한다. id_base는 즉시 확정 구간에, 복합 인덱스는 집계·선정에 사용한다.
- 예약분 카운터는 유지하고 예약 등록도 행사 배타 잠금으로 동시 등록과 오픈 전환을 보호한다.

## 7. 검증 기준

- 일반 정원 100명에 1,000명이 동시에 신청해도 최종 ACCEPTED는 정확히 100명이다.
- 일반 정원 101명의 즉시 확정 목표는 50명, 최종 확정 정원은 101명이다.
- 즉시 성공 응답을 받은 신청은 스케줄러 처리 후에도 반드시 ACCEPTED다.
- 중복 신청·롤백·다른 행사·예약 등록으로 ID에 빈 번호가 생겨도 최종 정원을 채운다.
- 작은 ID 신청의 커밋이 지연되어도 결과 공개 전에 해당 신청이 반영된다.
- 마감 중 유입된 요청이 결과 확정 후 PENDING으로 뒤늦게 남지 않는다.
- 스케줄러 동시 실행·중간 실패·재시작 후에도 성공 인원과 최종 결과가 바뀌지 않는다.
- 정원 미달 시간 마감, 일반 정원 0명·1명, 예약분과 일반분 교차 중복을 검증한다.
- PENDING·REJECTED 티켓은 QR 스캔에서 거절되고 ACCEPTED만 기존 수령 흐름으로 이어진다.

## 8. 운영과 API 전환

- `NIGHT_SNACK_SCHEDULER_ENABLED`: 기본 true. 테스트에서만 false로 설정하고 LifecycleService를 직접 호출한다.
- `NIGHT_SNACK_SCHEDULER_DELAY_MS`: 기본 1000. 폴링 권장 간격도 1초이며 최종 상태가 되면 종료한다.
- `COMIT_NIGHT_SNACK_RESERVATION_STRATEGY`는 제거했다.
- 클라이언트는 신청 HTTP 200을 최종 성공으로 해석하지 말고 selectionStatus를 확인한다.
- PENDING·REJECTED에는 ticketToken이 null이다. 관리자 조회의 applied도 ACCEPTED일 때만 true다.
- API 필드와 remaining 컬럼이 변경되므로 서버·클라이언트 전환을 함께 적용한다.
- [검증 가이드](../guides/how-to-test-nightsnack.md)를 따른다.

## 근거

- [MySQL: Consistent Nonlocking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html)
- [MySQL: AUTO_INCREMENT Handling](https://dev.mysql.com/doc/refman/8.4/en/innodb-auto-increment-handling.html)
- [MySQL: Locking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)
