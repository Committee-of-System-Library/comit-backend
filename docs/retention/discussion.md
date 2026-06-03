## 비즈니스 요구사항

### 배경
기존 야식마차는 학생들이 30~60분 전부터 오프라인 줄을 서는 문제가 있었다. 이를 해결하기 위해 모바일 기반 선착순 신청 시스템을 도입하고, 오프라인 대기열을 온라인으로 전환한다. 야식마차 직접 도입 전 간식마차를 시범 운영하여 리스크를 검증한다. 신청자 정보에 대해서는 현장 QR에서 정보를 한번 더 수집한다. 전체 인원은 약 1,400명이고 아마 동시에 400명 정도는 확정적으로 접근할 것으로 예상됩니다.

현재 시도위 온프레미스 단일 서버(10Core 32GB Ram)를 활용하고 있습니다.

> 지금까지 아래에 데이터 모델링 방법 2개와 동시성 처리 전략에 대해서 고민하고 있습니다. 모델링 방법 1개와 동시성 처리 전략 1개를 선정하는 형태로 답변을 주시면 감사하겠습니다.

### 운영 정책

| 항목 | 내용 |
|------|------|
| 운영 기간 | 3일 (매일 독립 신청) |
| 1일 정원 | 100명 or 150명 (고정, 변경 없음) |
| 신청 오픈 | 매일 17:30 정각 |
| 수령 시간 | 18:00 ~ 18:30 |
| 노쇼 처리 | DB 기록만 (페널티 없음) |
| 중복 신청 | 동일 날짜 1인 1회, 3일 각각 독립 신청 가능 |

> 야식마차와 간식마차는 하루에 2개를 다 할 가능성은 0으로 보고 하루에 하나의 행사를 진행한다.

### 핵심 기능
- **선착순 신청:** 17:30 정각 버튼 활성화, 마감 시 즉시 비활성화
- **QR 티켓 발급:** 신청 성공 시 고유 토큰 기반 QR 화면 제공
- **수령 확인:** 관리자가 QR 스캔으로 수령 처리
    - 18:00 ~ 18:20: 신청자만 스캔 가능
    - 18:20 이후: 미신청자도 스캔 가능 (잔여 물량 현장 배부)
- **노쇼 자동 만료:** 18:30 이후 미수령 티켓 자동 만료 처리


## 데이터 모델링 전략

----

### 모델 A — events 테이블 포함

```
members (1) ──── (N) applications (N) ──── (1) events
```

```sql
events
├── id
├── event_date   DATE
└── status       ENUM(SCHEDULED, OPEN, CLOSED)

applications
├── id
├── member_id    FK → members
├── event_id     FK → events
├── ticket_token VARCHAR
├── status       ENUM(PENDING, CONFIRMED, NO_SHOW)
└── confirmed_at DATETIME

UNIQUE (member_id, event_id)
```

**적합한 상황**
- 이벤트 상태(OPEN/CLOSED)를 DB에서 직접 관리하고 싶을 때
- 정원 초과 방지를 DB 레벨에서 처리하고 싶을 때

**어울리는 동시성 전략:** 방안 1 (DB Atomic UPDATE), 방안 3 (DB SKIP LOCKED)

---

### 모델 B — events 테이블 없음

```
members (1) ──── (N) applications
```

```sql
applications
├── member_id    FK → members   -- PK
├── event_date   DATE           -- PK (이벤트 구분자)
├── ticket_token VARCHAR
├── status       ENUM(PENDING, CONFIRMED, NO_SHOW)
└── confirmed_at DATETIME

PRIMARY KEY (member_id, event_date)
```

**선택 근거**
- 정원·오픈시간이 고정값 → DB에 저장할 이유 없음
- 정원 변경·삭제 없음 → 정규화 실익 없음
- 이벤트 통계는 `GROUP BY event_date` COUNT 쿼리로 충분
- 정원 초과 방지는 WAS 또는 Redis 레벨에서 처리

**어울리는 동시성 전략:** 방안 2 (Redis Lua Script), 방안 4 (선착순 처리 컨테이너 분리), 방안 5 (WAS 슬롯 풀)

---

### 모델 비교

| | 모델 A | 모델 B |
|--|--------|--------|
| events 테이블 | 있음 | 없음 |
| 테이블 수 | 3개 | 2개 |

---

## 동시성 해결 전략

### 문제 정의

오후 5:30 신청 오픈 시, 수백 명이 동시에 "신청하기"를 누르는 상황에서 **Race Condition**이 발생한다.

```
잔여 수량 조회 (remaining = 100)
         ↓
잔여 수량 감소 (remaining - 1)
```

이 두 단계 사이에 다른 요청이 끼어들면 100명 정원인데 150명이 동시에 성공 응답을 받을 수 있다.

---

### 방안 1 — DB 원자적 UPDATE 선점 전략

> **모델 A 전용** (events.remaining 필요)

**핵심 아이디어:** `WHERE remaining > 0` 조건을 포함한 단일 UPDATE로 DB 엔진이 원자적으로 처리하게 위임한다. 조회와 감소를 두 단계로 분리하지 않고 하나의 쿼리로 합쳐 Race Condition 자체를 제거한다.

```sql
UPDATE events
SET remaining = remaining - 1
WHERE id = :event_id AND remaining > 0;
```

- `affected_rows = 1` → 선점 성공, applications INSERT 진행
- `affected_rows = 0` → 마감, HTTP 409 반환

**장점**
- 추가 인프라 없음
- 구현 2줄, 가장 단순
- 다중 컨테이너 환경에서도 DB가 직렬화 처리

**단점**
- 모든 요청이 DB를 한 번씩 거침 (핫스팟 row 부하)
- 이 규모(100~150명 정원, 수백~수천 경쟁)에서는 충분히 감당 가능

---

### 방안 2 — Redis Lua 스크립트 원자적 선점 전략

> **모델 A / B 모두 가능** (remaining을 Redis가 관리)

**핵심 아이디어:** Redis는 싱글 스레드로 Lua 스크립트를 원자적으로 실행한다. 잔여 수량 확인 + 감소 + 중복 신청 확인을 하나의 스크립트 안에서 처리해 Race Condition과 중복 신청을 동시에 차단한다.

```lua
-- check_and_reserve.lua
local remaining_key = KEYS[1]
local applied_key   = KEYS[2]
local student_id    = ARGV[1]

if redis.call('SISMEMBER', applied_key, student_id) == 1 then
    return -2  -- 이미 신청한 학생
end

local current = tonumber(redis.call('GET', remaining_key))
if current == nil or current <= 0 then
    return -1  -- 마감
end

redis.call('DECR', remaining_key)
redis.call('SADD', applied_key, student_id)
return current - 1
```

```java
Long result = redisTemplate.execute(
    redisScript,
    List.of("remaining:event:" + eventDate, "applied:event:" + eventDate),
    studentId
);

if (result == -2) return ResponseEntity.status(409).body("이미 신청했습니다");
if (result == -1) return ResponseEntity.status(409).body("마감");

applicationRepository.save(memberId, eventDate, ticketToken);
return ResponseEntity.ok();
```

**장점**
- Race Condition 원천 차단
- DB는 당첨자(100~150명)만 접근
- 중복 신청 방지까지 단일 스크립트 처리

**단점**
- Redis 컨테이너 추가 필요
- Redis 장애 시 신청 불가 (SPOF)
- Lua 성공 후 DB 저장 실패 시 불일치 → 보상 로직 필요

---

### 방안 3 — DB SKIP LOCKED 분산 슬롯 선점 전략

> **모델 A 변형** (slots 테이블로 대체, events.remaining 불필요)

**핵심 아이디어:** 슬롯 100개를 행으로 미리 생성한다. `FOR UPDATE SKIP LOCKED`로 각 요청이 서로 다른 행을 경합 없이 선점한다. 이미 락이 걸린 행을 건너뛰기 때문에 대기(blocking) 없이 병렬 처리된다.

```sql
-- 이벤트 생성 시 슬롯 100개 사전 적재
INSERT INTO slots (event_date, ticket_token)
VALUES ('2026-05-20', UUID()), ... -- 100번 반복

-- 신청 요청 시
BEGIN;

SELECT id, ticket_token FROM slots
WHERE event_date = :event_date AND member_id IS NULL
LIMIT 1
FOR UPDATE SKIP LOCKED;

-- 결과 없음 → 마감
UPDATE slots
SET member_id = :member_id
WHERE id = :slot_id;

COMMIT;
```

**장점**
- 추가 인프라 없음
- 경합 없이 각 트랜잭션이 다른 슬롯 선점 → 높은 처리량
- 슬롯 행이 곧 티켓 → ticket_token 별도 관리 불필요

**단점**
- MySQL 8.0+ / PostgreSQL 9.5+ 필요
- 슬롯 사전 생성 로직 필요
- 노쇼 반환 시 `member_id = NULL` 복구 로직 필요

---

### 방안 4 — 선착순 처리 컨테이너 분리 전략

> **모델 B 전용** (단일 컨테이너 + WAS 메모리)

**핵심 아이디어:** Nginx에서 `/api/apply` 엔드포인트만 단일 App 컨테이너로 고정 라우팅한다. 해당 컨테이너 내에서 `AtomicInteger`로 인메모리 선점 처리하고, 선점 성공한 요청만 DB에 기록한다.

```nginx
upstream app_reservation {
    server app_a:8080;  # 신청은 app_a 단일 컨테이너로만
}

location /api/apply {
    proxy_pass http://app_reservation;
}
```

```java
private final AtomicInteger remaining = new AtomicInteger(100);

@PostConstruct
void init() {
    int count = applicationRepository.countByEventDate(LocalDate.now());
    remaining.set(100 - count);  // 재시작 시 복구
}

public ResponseEntity<?> apply(String memberId) {
    if (remaining.decrementAndGet() >= 0) {
        try {
            applicationRepository.save(memberId, LocalDate.now(), UUID.randomUUID().toString());
            return ResponseEntity.ok();
        } catch (Exception e) {
            remaining.incrementAndGet();  // DB 실패 시 롤백
            throw e;
        }
    }
    remaining.incrementAndGet();
    return ResponseEntity.status(409).body("마감");
}
```

**장점**
- 추가 인프라 없음
- 인메모리 처리라 DB는 당첨자만 접근
- 나머지 API는 기존 로드밸런싱 유지

**단점**
- app_a 장애 시 신청 불가 (SPOF)
- 재시작 시 DB 기반 복구 필요

---

### 방안 5 — WAS 슬롯 풀 선점 전략

> **모델 B 전용** (단일 WAS + 사전 생성 토큰 큐)

**핵심 아이디어:** WAS 시작 시 100개의 티켓 토큰을 `ConcurrentLinkedQueue`에 미리 적재한다. 신청 요청은 `poll()`로 토큰 하나를 원자적으로 꺼내고, DB에 기록한다. 토큰이 곧 QR 코드 값이 된다.

```java
@Component
public class SlotPool {
    private final ConcurrentLinkedQueue<String> slots = new ConcurrentLinkedQueue<>();

    @PostConstruct
    void init() {
        // 재시작 시 DB에서 미사용 토큰 복구
        List<String> remaining = applicationRepository.findUnusedTokens(LocalDate.now());
        if (!remaining.isEmpty()) {
            slots.addAll(remaining);
            return;
        }
        // 신규 이벤트: 토큰 100개 생성
        IntStream.range(0, 100)
            .mapToObj(i -> UUID.randomUUID().toString())
            .forEach(slots::offer);
    }

    public String poll() {
        return slots.poll();  // null이면 마감
    }
}
```

```java
public ResponseEntity<?> apply(String memberId) {
    String token = slotPool.poll();
    if (token == null) return ResponseEntity.status(409).body("마감");

    try {
        applicationRepository.save(memberId, LocalDate.now(), token);
        return ResponseEntity.ok(token);
    } catch (Exception e) {
        slots.offer(token);  // DB 실패 시 토큰 반환
        throw e;
    }
}
```

**장점**
- 추가 인프라 없음
- `poll()`이 원자적 — AtomicInteger보다 "슬롯 하나 꺼내기" 개념에 직관적
- 토큰 = QR 코드 값으로 바로 사용
- DB는 당첨자만 접근

**단점**
- 단일 WAS 전제 (다중 컨테이너 시 슬롯 풀 분리됨)
- 재시작 시 DB 기반 복구 필요

---

## 전략 비교

| 방안 | 적합한 모델 | 추가 인프라 | 다중 컨테이너 | DB 부하 |
|------|------------|------------|--------------|---------|
| DB Atomic UPDATE | A | 없음 | 가능 | 전체 요청 |
| Redis Lua Script | A / B | Redis | 가능 | 당첨자만 |
| DB SKIP LOCKED | A 변형 | 없음 | 가능 | 전체 요청 |
| 컨테이너 분리 (AtomicInteger) | B | 없음 | 우회 | 당첨자만 |
| WAS 슬롯 풀 (ConcurrentLinkedQueue) | B | 없음 | 우회 | 당첨자만 |

---


지금까지의 비즈니스적 명세로 봤을때 어떤 전략의 조합이 가장 타당할지 고민이 됩니다.