# 야식마차 로컬 수동 테스트

직접 서버를 실행하고 `INSERT 접수 → 즉시 확정/결과 대기 → 스케줄러 마감 → 최종 결과 조회`를 확인하는 절차다.
프로젝트 루트에서 시작하며, 로컬 Docker DB와 `local` 프로필을 사용한다.

## 1. 테스트 구성

| 항목 | 이번 테스트 값 |
|---|---|
| 서버 | `http://localhost:53080` |
| MySQL | `comit-local-mysql`, DB `comit`, root/root |
| 일반 정원 | 4명 |
| 예약분 | 0명 (`COMIT_NIGHT_SNACK_RESERVED_CAPACITY_RATIO=0`) |
| 신청자 | 서로 다른 테스트 회원 6명 |
| 스케줄러 간격 | 15초. 대기 상태를 눈으로 확인할 시간을 확보한다. 운영 기본값은 1초다. |
| 예상 결과 | 최종 ACCEPTED 4명, REJECTED 2명, PENDING 0명 |

필요한 것은 Docker, 프로젝트 Java 21 도구 체인, Python 3, curl이다. jq나 실제 SSO 로그인은 필요하지 않다.
로컬 인증 브리지에 테스트 회원의 `X-Member-Sub`를 전달한다.

## 2. 터미널 A: DB와 서버 실행

```bash
cd /Users/keephun/IdeaProjects/comit
docker compose -f compose.local.yml up -d
docker compose -f compose.local.yml ps
```

MySQL이 healthy인 것을 확인한다. 이후 같은 터미널에서 서버를 실행한다.

```bash
TZ=Asia/Seoul \
JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Seoul \
NOTICE_SCHEDULER_ENABLED=false \
COMIT_NIGHT_SNACK_RESERVED_CAPACITY_RATIO=0 \
NIGHT_SNACK_SCHEDULER_ENABLED=true \
NIGHT_SNACK_SCHEDULER_DELAY_MS=15000 \
S3_BUCKET_NAME=local-test \
S3_ACCESS_KEY=local-test \
S3_SECRET_KEY=local-test \
./gradlew bootRun --args='--spring.profiles.active=local'
```

`Started ComitApplication` 로그를 확인하고 이 터미널을 열어 둔다.
`local` 프로필은 로컬 AI 대체 구현과 placeholder API key를 사용하므로 이 테스트에 실제 OpenAI 키는 필요하지 않다.
이미 서버가 실행 중이면 중지하고 위 환경변수로 다시 실행해야 예약분·스케줄러 설정이 반영된다.

최초 실행 시 Flyway가 V115·V116까지 반영한다. V116은 선정 상태를 추가하고 일반 remaining 컬럼을 제거한다.
이후에는 새 서버 코드로 실행한다. `.env.local.example`을 복사하는 것만으로 bootRun에 환경변수가 자동 주입되지는 않는다.

## 3. 터미널 B: 접속과 테스트 계정 준비

아래부터는 두 번째 터미널에서 순서대로 실행한다. 이후 명령에서 사용하는 변수와 함수를 이 터미널에 유지한다.

```bash
cd /Users/keephun/IdeaProjects/comit
export TZ=Asia/Seoul
BASE_URL=http://localhost:53080
curl -fsS "$BASE_URL/actuator/health"
```

로컬 DB에 관리자 1명과 학생 6명을 등록한다. 다음 명령은 테스트 이름의 회원을 추가하며 기존 신청 데이터는 삭제하지 않는다.

```bash
python3 - <<'PY' | docker exec -i comit-local-mysql mysql -uroot -proot comit
rows = [("ns-manual-admin", "nsmanualadmin", "2999000000", "ADMIN")]
rows += [(f"ns-manual-user-{i}", f"nsmanual{i}", f"299900000{i}", "STUDENT") for i in range(1, 7)]
print("INSERT IGNORE INTO member "
      "(sso_sub, nickname, name, phone, student_number, student_number_visible, "
      "status, comit_role, created_at, agreed_at) VALUES")
print(",\n".join(
    f"('{sub}', '{nickname}', '수동테스트', '010-0000-0000', '{student}', "
    f"true, 'ACTIVE', '{role}', NOW(), NOW())"
    for sub, nickname, student, role in rows
) + ";")
PY

docker exec comit-local-mysql mysql -uroot -proot comit -e \
"SELECT sso_sub, student_number, comit_role, status
 FROM member WHERE sso_sub LIKE 'ns-manual-%' ORDER BY sso_sub;"
```

`ns-manual-admin`은 ADMIN, 학생 6명은 STUDENT이며 모두 ACTIVE여야 한다.
`X-Member-Role: ADMIN`이나 개발 로그인 API의 `role=ADMIN`으로 권한을 부여할 수 없다. 실제 권한은 DB의 `comit_role`이다.
등록되지 않은 sub를 임의로 보내면 자동 가입되지 않고 `REGISTRATION_REQUIRED`가 발생한다.

API 호출용 함수를 등록한다.

```bash
admin_api() {
  curl -fsS -H 'X-Member-Sub: ns-manual-admin' \
    -H 'X-Member-Name: ManualAdmin' \
    -H 'X-Member-Student-Number: 2999000000' "$@"
}

student_api() {
  local student_index="$1"
  shift
  curl -sS -H "X-Member-Sub: ns-manual-user-${student_index}" \
    -H "X-Member-Name: ManualUser${student_index}" \
    -H "X-Member-Student-Number: 299900000${student_index}" "$@"
}

admin_api "$BASE_URL/admin/night-snacks" | python3 -m json.tool
```

마지막 호출이 200이어야 한다. `FORBIDDEN`이면 위 SQL 결과의 관리자 권한을 먼저 확인한다.

## 4. 테스트 행사 생성과 자동 오픈

행사를 만들 때마다 사용하지 않은 행사 날짜를 선택한다. 신청 가능 시간은 행사 날짜와 별도로 현재 시각 기준으로 설정한다.
아래 함수를 등록하면 이후 재시험할 때도 같은 함수를 사용할 수 있다.

```bash
new_snack() {
  local test_capacity="${1:-4}"
  local test_duration_seconds="${2:-300}"
  local test_date
  test_date=$(docker exec comit-local-mysql mysql -uroot -proot -N -B comit -e \
    "SELECT DATE_FORMAT(GREATEST(CURRENT_DATE(),
       COALESCE(DATE_ADD(MAX(night_snack_date), INTERVAL 1 DAY), CURRENT_DATE())),
       '%Y-%m-%d') FROM night_snack;")

  python3 - "$test_date" "$test_capacity" "$test_duration_seconds" <<'PY' > /tmp/comit-night-snack-manual-create.json
import json, sys
from datetime import datetime, timedelta
now = datetime.now().replace(microsecond=0)
print(json.dumps({
    "nightSnackDate": sys.argv[1],
    "capacity": int(sys.argv[2]),
    "openAt": (now - timedelta(seconds=5)).isoformat(),
    "closeAt": (now + timedelta(seconds=int(sys.argv[3]))).isoformat(),
    "title": "야식마차 수동 테스트",
    "menu": "테스트 메뉴",
    "pickupLocation": "테스트 장소",
    "requiresStudentCouncilFee": False
}, ensure_ascii=False))
PY

  NIGHT_SNACK_ID=$(admin_api -X POST "$BASE_URL/admin/night-snacks" \
    -H 'Content-Type: application/json' \
    --data-binary @/tmp/comit-night-snack-manual-create.json \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["nightSnackId"])')
  export NIGHT_SNACK_ID
  echo "NIGHT_SNACK_ID=$NIGHT_SNACK_ID"
}

new_snack 4 300
```

일반 정원 4명, 지금부터 5분간 접수하는 행사가 생성된다.
아래 명령을 반복해 해당 ID가 OPEN이 되는지 확인한다. 자동 오픈은 최대 약 15초 후에 실행된다.

```bash
admin_api "$BASE_URL/admin/night-snacks" | python3 -c '
import json, os, sys
items = json.load(sys.stdin)["data"]
print(json.dumps(next(x for x in items if x["nightSnackId"] == int(os.environ["NIGHT_SNACK_ID"])),
                 ensure_ascii=False, indent=2))'
```

`status=OPEN`, `capacity=4`, `reservedCapacity=0`을 확인한다.
이 확인 후 바로 다음 단계로 진행한다. 수동 오픈 API를 따로 호출할 필요는 없다.

## 5. 여섯 명 신청: 즉시 확정과 PENDING 확인

```bash
for student_index in 1 2 3 4 5 6; do
  echo "user=$student_index"
  student_api "$student_index" -X POST \
    "$BASE_URL/night-snacks/$NIGHT_SNACK_ID/applications" | python3 -m json.tool
done
```

ID에 빈 번호가 없고 다른 행사의 신청이 끼지 않았다면 앞의 2명은 ACCEPTED, 이후 4명은 PENDING으로 접수된다.
ID 공백이 있으면 즉시 확정 인원은 2명보다 적을 수 있다. 최종 선정에서는 실제 접수 행 기준으로 4명을 채운다.

PENDING 응답 예시:

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

신청 API의 200은 접수 완료를 뜻한다. PENDING에는 QR을 표시하지 않는다.
스케줄러가 6명 접수 전에 먼저 실행되면 뒤쪽 요청은 `EVENT_NOT_OPEN`일 수 있다.
6건 모두 접수된 상태의 탈락을 확인하려면 새 행사를 만들고 OPEN 확인 직후 신청 루프를 실행한다.

## 6. 최종 결과 조회와 SQL 확인

신청 루프를 끝낸 뒤 15초 정도 지나면 다시 조회한다.

```bash
for student_index in 1 2 3 4 5 6; do
  echo "user=$student_index"
  student_api "$student_index" \
    "$BASE_URL/night-snacks/$NIGHT_SNACK_ID/my-ticket" | python3 -m json.tool
done
```

6건 모두 마감 전에 접수됐다면 기대 결과는 다음과 같다.

| 확인 대상 | 기대 결과 |
|---|---|
| 앞선 실제 신청 4건 | `selectionStatus=ACCEPTED`, ticketToken 제공 |
| 정원 밖 신청 2건 | `selectionStatus=REJECTED`, ticketToken=null |
| 모든 신청 | `selectionStatus=PENDING`이 남아 있지 않음 |
| 즉시 성공한 신청 | 최종 선정 후에도 ACCEPTED |
| 수령 상태 `status` | QR 수령 전이므로 PENDING. 선정 대기를 뜻하지 않는다. |
| 행사 상태 | CLOSED |

DB에서도 확인한다.

```bash
docker exec comit-local-mysql mysql -uroot -proot comit -e \
"SELECT id, status, capacity, reserved_capacity FROM night_snack WHERE id=$NIGHT_SNACK_ID;
 SELECT source, selection_status, COUNT(*) AS count
 FROM night_snack_application WHERE night_snack_id=$NIGHT_SNACK_ID
 GROUP BY source, selection_status;
 SELECT id, student_number, selection_status, status
 FROM night_snack_application WHERE night_snack_id=$NIGHT_SNACK_ID ORDER BY id;"
```

GENERAL ACCEPTED 4건, REJECTED 2건이어야 한다.
마감 전에는 즉시 확정자도 DB의 `selection_status`가 PENDING일 수 있다.
즉시 확정은 불변 ID 구간으로 계산해 API에서 제공하고, 마감할 때 DB에 최종 당락을 저장하기 때문이다.

## 7. 추가로 확인할 경우

### 마감 후 새 신청 차단

```bash
student_api 1 -X POST "$BASE_URL/night-snacks/$NIGHT_SNACK_ID/applications" | python3 -m json.tool
```

이미 접수한 회원이므로 `ALREADY_APPLIED`가 우선 반환될 수 있다.
마감 때문에 차단되는 것을 확인하려면 미신청 테스트 계정이 필요하다.
예를 들어 `X-Member-Sub: dev-user-001`인 기존 시드 회원으로 아래를 호출한다.

```bash
curl -sS -X POST "$BASE_URL/night-snacks/$NIGHT_SNACK_ID/applications" \
  -H 'X-Member-Sub: dev-user-001' \
  -H 'X-Member-Student-Number: 2021000001' | python3 -m json.tool
```

해당 회원이 이 행사에 신청하지 않았다면 `EVENT_NOT_OPEN`이 반환되어야 한다.

### 정원 미달이어도 시간 마감으로 확정되는지

```bash
new_snack 10 45
```

OPEN을 확인한 뒤 학생 1~2명만 신청한다. `new_snack` 실행 후 약 45초와 스케줄러 실행 여유 15초가 지나면
정원 10명을 채우지 못했어도 행사는 CLOSED, 접수된 신청은 모두 ACCEPTED여야 한다.
그동안 조회 응답이 PENDING이어도 실패로 판단하지 않는다.

### 같은 회원 중복 접수

새 행사에서 같은 학생의 신청 API를 두 번 호출한다. 두 번째는 `ALREADY_APPLIED`이고 DB에는 1건만 있어야 한다.

### 예약분도 함께 시험하려면

서버를 중지하고 `COMIT_NIGHT_SNACK_RESERVED_CAPACITY_RATIO=0.25`로 재시작한다.
정원 4명 행사는 예약분 1명·일반분 3명이다. 일반 신청은 최종 3명만 확정돼야 한다.
사전신청 등록은 SCHEDULED 상태에서만 가능하므로, 예약 등록까지 확인하려면 openAt을 미래로 지정한다.
등록 API 계약은 [관리자 API 정의](../../src/main/java/kr/ac/knu/comit/nightsnack/controller/api/AdminNightSnackControllerApi.java)를 참고한다.

## 8. 문제 발생 시

| 증상 | 확인할 것 |
|---|---|
| `REGISTRATION_REQUIRED` | SQL로 등록한 sso_sub와 X-Member-Sub가 같은지 확인 |
| `FORBIDDEN` | 관리자 계정의 DB comit_role이 ADMIN인지 확인 |
| `EVENT_NOT_OPEN` | OPEN 상태 및 openAt <= 서버 시각 < closeAt 확인. 이미 정원 마감됐을 수도 있음 |
| `STUDENT_COUNCIL_FEE_REQUIRED` | 생성 요청의 requiresStudentCouncilFee=false 확인 |
| 생성 시 날짜 중복 오류 | 이전 변수를 재사용하지 말고 new_snack으로 새 날짜 생성 |
| PENDING이 계속 남음 | 스케줄러 활성화, 접수 건수 >= 일반 정원 또는 closeAt 경과 여부, 서버 에러 로그 확인 |
| 즉시 확정 인원이 절반보다 적음 | ID 공백·다른 행사 신청 여부 확인. 최종 선정 인원으로 정원 보장 확인 |
| 예약분이 0명이 아님 | 예약분 비율 환경변수와 서버 재시작 여부 확인 |
| DB 마이그레이션 오류 | V115·V116 적용 여부와 이전 서버 동시 실행 여부 확인 |

서버 로그는 터미널 A 또는 `logs/app-local.log`에서 확인한다.
특히 `야식마차 상태 갱신 실패: nightSnackId=...` 로그가 있으면 해당 예외를 확인한다.

## 9. 자동 테스트와 종료

실제 MySQL 동시성·중복·롤백 복구는 다음 명령으로 별도 검증한다.

```bash
JAVA_TOOL_OPTIONS=-Dapi.version=1.44 ./gradlew test \
  --tests 'kr.ac.knu.comit.nightsnack.*' \
  --tests 'kr.ac.knu.comit.api.NightSnackApiWebTest'
```

Testcontainers는 별도 임시 MySQL을 사용한다. 수동 실행 서버와 compose DB를 종료하지 않아도 된다.
Docker 29에서 구형 Docker API 오류를 피하도록 위 명령에 호환 옵션을 포함했다.

수동 시험이 끝나면 터미널 A에서 Ctrl+C로 서버를 종료한다. DB와 Qdrant도 종료하려면 실행한다.

```bash
docker compose -f compose.local.yml down
```

이 명령은 로컬 볼륨과 테스트 데이터를 보존한다. 재시험할 때는 서버를 다시 실행하고 `new_snack`으로 새 행사를 만든다.
