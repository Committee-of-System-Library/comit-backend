# MySQL Lock & SKIP LOCKED 학습 정리

## 목차

1. [트랜잭션 이상 현상](#1-트랜잭션-이상-현상)
2. [격리 수준 4단계](#2-격리-수준-4단계)
3. [InnoDB 락 종류와 계층](#3-innodb-락-종류와-계층)
4. [MVCC와 Consistent Read](#4-mvcc와-consistent-read)
5. [Locking Read](#5-locking-read--for-update--for-share)
6. [SKIP LOCKED 동작 원리](#6-skip-locked-동작-원리)
7. [선착순 시스템 응용](#7-선착순-시스템-응용)

---

## 1. 트랜잭션 이상 현상

락과 격리 수준은 아래 현상들을 막기 위해 존재한다.

| 현상 | 설명 | 예시 |
|------|------|------|
| **Dirty Read** | 커밋 안 된 데이터를 다른 트랜잭션이 읽음 | A가 remaining=99로 UPDATE했지만 COMMIT 전인데 B가 99를 읽음 |
| **Non-Repeatable Read** | 같은 쿼리를 두 번 실행했을 때 결과가 다름 | A가 remaining=100을 읽은 사이 B가 COMMIT → A가 다시 읽으면 99 |
| **Phantom Read** | 같은 조건의 SELECT인데 행 수가 달라짐 | A가 `WHERE status='available'` 조회 중 B가 새 행 INSERT → 재조회 시 결과 다름 |

---

## 2. 격리 수준 4단계

```
READ UNCOMMITTED  ← 가장 느슨
READ COMMITTED    ← Oracle 기본값
REPEATABLE READ   ← MySQL InnoDB 기본값  ★
SERIALIZABLE      ← 가장 엄격
```

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read |
|----------|-----------|---------------------|--------------|
| READ UNCOMMITTED | 발생 | 발생 | 발생 |
| READ COMMITTED | 방지 | 발생 | 발생 |
| **REPEATABLE READ** | **방지** | **방지** | 표준상 발생, InnoDB는 Next-Key Lock으로 대부분 방지 |
| SERIALIZABLE | 방지 | 방지 | 방지 |

> `NON-REPEATABLE READ`는 격리 수준이 아니라 **이상 현상**이다.
> MySQL 기본값은 `REPEATABLE READ`이고, 이 수준이 Non-Repeatable Read 현상을 *방지*한다.

---

## 3. InnoDB 락 종류와 계층

### 3-1. S락 / X락 기본

```
S Lock (Shared, 공유)  : 읽기 — 여러 트랜잭션 동시 획득 가능
X Lock (Exclusive, 배타): 쓰기 — 단 하나의 트랜잭션만 획득 가능

S + S → 호환 (동시 허용)
S + X → 비호환 (대기)
X + X → 비호환 (대기)
```

### 3-2. 의도 락 (Intention Lock) — 테이블 수준

행 락을 걸기 전에 테이블에 "곧 행 락을 걸 것"을 표시하는 락.
테이블 전체 락(`LOCK TABLE`)과의 충돌을 빠르게 감지하기 위해 존재한다.

```
IS Lock : 행에 S락 걸기 전 테이블에 표시
IX Lock : 행에 X락 걸기 전 테이블에 표시
```

`SELECT FOR UPDATE` 실행 시 내부 순서:
```
1. 테이블에 IX Lock 획득
2. 대상 행에 X Lock 획득
```

### 3-3. 행 수준 락 3종

```
인덱스: ... 1 | 5 | 10 | 20 ...
              ↑         ↑
         Record Lock  Next-Key Lock
```

**Record Lock**
- 인덱스 레코드 자체에만 락
- `id = 5` → 5번 행만 잠금

**Gap Lock**
- 레코드 사이의 빈 공간에 락
- `id = 5`의 갭 락 → (1, 5) 구간에 신규 INSERT 차단
- Phantom Read 방지 목적

**Next-Key Lock = Record Lock + Gap Lock**
- InnoDB가 기본으로 사용하는 형태
- `id = 5`의 Next-Key Lock → (-∞, 5] 구간 잠금
- REPEATABLE READ에서 `FOR UPDATE` 실행 시 자동 적용

### 3-4. 삽입 의도 락 (Insert Intention Lock)

INSERT 직전에 갭에 획득하는 특수한 갭 락.
서로 다른 위치에 삽입하려는 트랜잭션끼리는 충돌하지 않는다.

```
T1: id=3 INSERT → 갭 (1,5)에 Insert Intention Lock
T2: id=4 INSERT → 갭 (1,5)에 Insert Intention Lock
→ 삽입 위치가 다르므로 충돌 없이 동시 진행
```

| | Gap Lock | Insert Intention Lock |
|--|---------|----------------------|
| **Gap Lock** | 호환 | 비호환 (INSERT 차단) |
| **Insert Intention Lock** | 비호환 | 호환 (서로 허용) |

---

## 4. MVCC와 Consistent Read

InnoDB는 모든 변경에 대해 **Undo Log**에 이전 버전을 기록한다.
일반 SELECT는 락 없이 Undo Log의 스냅샷을 읽는다.

```
T_A 시작 (ID = 100)
  → SELECT remaining → 100 읽음

T_B가 remaining = 99로 UPDATE + COMMIT

T_A가 다시 SELECT
  → Undo Log에서 ID=100 이전 버전을 읽음 → 여전히 100
```

**핵심 구분:**

```
일반 SELECT           → MVCC 스냅샷 읽기 (락 없음, 과거 버전 가능)
SELECT ... FOR UPDATE → Locking Read (현재 최신 데이터 + X락)
```

---

## 5. Locking Read — FOR UPDATE / FOR SHARE

```sql
SELECT ... FOR SHARE    -- S Lock 획득 (다른 트랜잭션의 X락 차단)
SELECT ... FOR UPDATE   -- X Lock 획득 (다른 트랜잭션의 S락, X락 모두 차단)
```

REPEATABLE READ에서 `FOR UPDATE`를 실행하면 **Next-Key Lock**이 걸린다.

```sql
-- slots 테이블에 id = 1, 5, 10이 있을 때
SELECT id FROM slots WHERE status = 'available' LIMIT 1 FOR UPDATE;

-- 적용되는 락:
-- Record Lock : id=1 행
-- Gap Lock    : (-∞, 1), (1, 5) 구간
-- → 이 범위에 새 행 INSERT 불가
```

### NOWAIT vs SKIP LOCKED

```sql
FOR UPDATE NOWAIT      -- 락 획득 실패 시 즉시 에러 반환
FOR UPDATE SKIP LOCKED -- 락 걸린 행은 건너뛰고 다음 가용 행 선점
```

---

## 6. SKIP LOCKED 동작 원리

### 핵심 — 해결하는 문제는 갭 락이 아니라 행 락 경합(Blocking)

**SKIP LOCKED 없이 일반 UPDATE만 쓸 때:**

```
T1: id=1 찾음 → X Lock 획득 → UPDATE 실행 중
T2: id=1 찾음 → X Lock 시도 → T1이 끝날 때까지 대기 (Blocking)
T3: 같은 상황 → 줄 서서 대기
```

5:30 오픈 순간 수백 명이 id=1에 몰리면 줄 서기가 발생한다.

**SKIP LOCKED 사용 시:**

```
T1: id=1 X Lock 획득
T2: id=1 이미 잠김 → SKIP → id=2 X Lock 획득  ← 대기 없음
T3: id=1,2 잠김  → SKIP → id=3 X Lock 획득  ← 대기 없음
```

각 트랜잭션이 서로 다른 행을 잡으니 경합 자체가 없어진다.

### 갭 락과의 관계

갭 락은 SKIP LOCKED 사용 여부와 무관하게 `FOR UPDATE` 자체에서 발생한다.
슬롯을 미리 INSERT해두는 구조에서는 신규 행 삽입이 없으므로 갭 락이 실질적으로 문제가 되지 않는다.

| 이유 | 정확성 |
|------|--------|
| UPDATE는 X Lock으로 동시성 보호 | ✅ |
| 갭 락 때문에 SKIP LOCKED 필요 | ❌ |
| **행 락 경합(Blocking) 때문에 SKIP LOCKED 필요** | ✅ |

### 특징

- 결과가 **비결정적** — 어떤 행이 반환될지 보장 안 됨 → 순서가 중요한 로직에는 부적합
- MySQL 8.0+, PostgreSQL 9.5+ 지원

---

## 7. 선착순 시스템 응용

### 슬롯 테이블 설계

```sql
CREATE TABLE slots (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id   BIGINT NOT NULL,
    status     ENUM('available', 'taken') NOT NULL DEFAULT 'available',
    student_id VARCHAR(20) NULL,
    INDEX idx_event_status (event_id, status)  -- 필수 인덱스
);
```

`(event_id, status)` 복합 인덱스가 없으면 SKIP LOCKED가 풀 스캔한다.

### 신청 트랜잭션

```sql
BEGIN;

SELECT id FROM slots
WHERE event_id = :event_id AND status = 'available'
LIMIT 1
FOR UPDATE SKIP LOCKED;

-- 결과 없음 → ROLLBACK + 마감 응답
-- 결과 있음 →

UPDATE slots
SET status = 'taken', student_id = :student_id
WHERE id = :slot_id;

COMMIT;
```

### 격리 수준 고려

SKIP LOCKED는 Locking Read이므로 MVCC 스냅샷이 아닌 **현재 최신 상태**를 본다.
REPEATABLE READ 하에서도 `FOR UPDATE`는 항상 현재 커밋된 데이터를 읽으므로 격리 수준 문제가 없다.

### 노쇼 슬롯 반환

```sql
-- 마감 시간 이후 노쇼 처리
UPDATE slots
SET status = 'available', student_id = NULL
WHERE event_id = :event_id
  AND status = 'taken'
  AND confirmed_at IS NULL
  AND NOW() > :deadline;
```

반환된 슬롯은 다시 SKIP LOCKED로 선점 가능해진다.

---

## 핵심 요약

> REPEATABLE READ는 MVCC로 일반 SELECT를 보호하고,
> FOR UPDATE는 현재 데이터에 X Lock을 걸며,
> SKIP LOCKED는 그 X Lock이 걸린 행을 건너뛰어
> **경합 없는 분산 선점**을 가능하게 한다.
