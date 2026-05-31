# 야식 마차(NightSnack) 선착순 신청 시스템 테스트 가이드

이 문서는 새롭게 리팩토링된 야식 마차(NightSnack) 선착순 신청 시스템의 API를 호출하고 테스트하는 방법을 안내합니다.

## 1. 개요
선착순 신청은 다음 3단계로 이루어집니다.
1. **관리자**: 야식 마차 이벤트 생성 (`SCHEDULED` 상태)
2. **관리자**: 야식 마차 신청 오픈 (`OPEN` 상태로 변경)
3. **일반 유저**: 오픈된 야식 마차에 선착순 신청

## 2. API 요청 방법 (curl 예시)

> **주의사항**:
> - 모든 API 요청은 로그인이 필요합니다. 헤더에 유효한 세션 쿠키(`JSESSIONID`) 또는 인증 토큰을 포함해야 합니다.
> - 아래 예시는 로컬 개발 환경(`localhost:53080`)을 기준으로 작성되었습니다.

### 단계 1: 야식 마차 생성 (관리자 전용)
관리자가 날짜와 정원(capacity)을 설정하여 새로운 야식 마차 이벤트를 생성합니다.

**요청 (Request)**
```bash
curl -X POST http://localhost:53080/admin/night-snacks \
-H "Content-Type: application/json" \
-b "JSESSIONID=관리자_세션_아이디" \
-d '{
  "nightSnackDate": "2026-05-20",
  "capacity": 100
}'
```

**응답 (Response)**
```json
{
  "result": "SUCCESS",
  "data": {
    "nightSnackId": 1
  }
}
```

### 단계 2: 야식 마차 오픈 (관리자 전용)
생성된 야식 마차(예: `nightSnackId: 1`)의 신청을 받기 위해 상태를 `OPEN`으로 변경합니다.

**요청 (Request)**
```bash
curl -X PATCH http://localhost:53080/admin/night-snacks/1/open \
-b "JSESSIONID=관리자_세션_아이디"
```

**응답 (Response)**
```json
{
  "result": "SUCCESS",
  "data": null
}
```

### 단계 3: 야식 마차 선착순 신청 (일반 유저)
오픈된 야식 마차에 일반 유저가 선착순으로 신청합니다.

**요청 (Request)**
```bash
curl -X POST http://localhost:53080/night-snacks/1/applications \
-b "JSESSIONID=일반유저_세션_아이디"
```

**응답 (Response) - 성공 시**
```json
{
  "result": "SUCCESS",
  "data": {
    "ticketToken": "9f1c2e4a-7b8d-4c2a-9e3f-1a2b3c4d5e6f",
    "sequence": 1,
    "remaining": 99
  }
}
```
* `ticketToken`: 야식 수령 시 사용할 고유 QR 티켓 값입니다.
* `sequence`: 본인의 대기 순번입니다.
* `remaining`: 현재 남은 수량입니다.

**응답 (Response) - 실패 예외 코드**
- `EVENT_NOT_FOUND`: 존재하지 않는 야식 마차 ID
- `EVENT_NOT_OPEN`: 아직 오픈되지 않았거나 이미 마감/종료된 경우
- `EVENT_SOLD_OUT`: 준비된 정원이 모두 소진된 경우
- `ALREADY_APPLIED`: 동일한 야식 마차에 이미 신청한 경우

---

## 3. 테스트용 계정 안내
`V100__dev_seed.sql`에 의해 생성된 로컬 개발용 테스트 계정을 사용하여 로그인 후 위 API를 테스트할 수 있습니다.

- **관리자 계정**: `dev-admin-001`
- **일반 유저 계정**: `dev-user-001`, `dev-user-002`

(로컬 개발 환경의 인증 로그인 방식에 따라 먼저 로그인을 수행하여 쿠키를 획득한 후 테스트를 진행해 주세요.)
