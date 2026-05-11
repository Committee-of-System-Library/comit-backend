# Comit MVP 운영을 위한 최소 행동 로그 수집 제안

> **서버 인프라를 확인한 결과, 백엔드 요청 추적은 이미 Pinpoint로 커버되고 있습니다.**  
> 실제로 남은 과제는 **Business Event 로깅**과 **프론트 PA 툴 도입** 두 가지입니다.

---

## 지금 우리가 모르는 것

Comit을 배포한 이후, 우리는 아래 질문에 답하지 못하고 있습니다.

- 학생들이 실제로 들어오고 있는가?
- 들어온 학생이 가입까지 이어지는가?
- 가입한 학생이 첫 글이나 댓글을 남기고 있는가?
- 검색과 인기글이 실제로 쓰이고 있는가?
- 배포 이후 참여가 좋아졌는가, 나빠졌는가?

"잘 되고 있겠지"는 운영 판단이 아닙니다.  
지금 우리가 갖고 있는 건 **서비스를 배포했다는 사실**뿐이고, **실제로 작동하고 있는지는 모릅니다.**

---

## 현재 인프라 상태

서버를 직접 확인한 결과, Pinpoint APM이 이미 comit-server에 붙어 있습니다.

```
-javaagent:/opt/pinpoint-agent/pinpoint-bootstrap.jar
-Dpinpoint.applicationName=comit-backend
-Dpinpoint.agentId=comit-stg-01
→ pinpoint-collector로 GRPC 전송 중
```

### Pinpoint가 이미 커버하는 것

| 항목 | 상태 |
|---|---|
| 요청별 trace_id 생성 및 전파 | ✅ Pinpoint 자동 처리 |
| method / path / status / latency 수집 | ✅ Pinpoint 자동 수집 |
| 서비스 간 요청 흐름 시각화 | ✅ Pinpoint Web UI (SSH 터널로 접근) |
| JVM 메트릭, 느린 쿼리 감지 | ✅ Pinpoint |

→ access log와 trace_id 직접 구현, Zipkin 도입은 **불필요**합니다.

---

## 실제로 남은 과제 두 가지

### 1. Business Event 로깅

Pinpoint는 요청 흐름을 추적하지만, **커뮤니티에서 어떤 활동이 발생했는지**는 기록하지 않습니다.

아래 이벤트는 직접 남겨야 합니다.

```
SSO_LOGIN_SUCCEEDED
MEMBER_REGISTERED
POST_CREATED
COMMENT_CREATED
POST_LIKED
REPORT_CREATED
```

이 로그가 있어야 아래 질문에 답할 수 있습니다.

- 이번 주 실제로 가입한 학생이 몇 명인가?
- 가입 후 첫 글을 남긴 비율은?
- 배포 전후로 게시글 생성이 달라졌는가?

**구현 원칙**
- `AFTER_COMMIT + @Async` — 로깅 실패가 서비스 실패가 되지 않도록 분리
- 게시글/댓글 본문 전문, 학번 원문, 이메일 원문은 저장하지 않음

---

### 2. 프론트 PA 툴 도입

#### Pinpoint가 보지 못하는 영역이 있다

Pinpoint는 **서버에 요청이 도달한 이후**만 봅니다.

학생이 Comit에 들어와서 회원가입 버튼을 찾지 못하고 그냥 나갔다면, Pinpoint에는 아무것도 찍히지 않습니다. 서버에 요청 자체가 가지 않았기 때문입니다.

즉 지금 우리에게는 **서버 앞에서 일어나는 일이 완전히 보이지 않습니다.**

#### 이게 왜 문제인가

시나리오를 보면 바로 이해됩니다.

**시나리오. 회원가입이 기대보다 적다**

Pinpoint로 알 수 있는 것:
- `POST /api/auth/register` 요청이 몇 번 들어왔는가

Pinpoint로 알 수 없는 것:
- 가입 페이지까지 왔다가 이탈한 학생이 몇 명인가
- 가입 버튼을 눌렀는데 에러 메시지를 보고 포기했는가
- 아예 가입 페이지를 찾지 못했는가
- 로그인 페이지에서 SSO 버튼을 못 찾고 나갔는가

서버 로그만으로는 **"요청을 보낸 사람"만 보입니다. 요청을 보내지 못하고 나간 사람은 존재 자체를 알 수 없습니다.**

개선을 하려면 어디가 문제인지 알아야 하는데, 프론트 행동 데이터 없이는 추측으로 개선하게 됩니다.

#### 수집 이벤트

```
page_view           — 어떤 페이지에 들어왔는가
login_started       — 로그인을 시도했는가
signup_started      — 가입 흐름에 진입했는가
search_executed     — 검색을 실제로 사용했는가
post_create_clicked — 글쓰기를 시도했는가
comment_create_clicked — 댓글을 시도했는가
hot_post_clicked    — 인기글을 클릭했는가
```

이 이벤트들을 Business Event와 연결하면 아래 퍼널이 완성됩니다.

```
login_started → SSO_LOGIN_SUCCEEDED → MEMBER_REGISTERED → POST_CREATED
                                                         → COMMENT_CREATED
```

이 퍼널이 보여야 "어느 단계에서 학생들이 빠지는지"를 알 수 있고, 그때 개선 우선순위가 생깁니다.

#### 툴 선택: PostHog Self-hosted

현재 `knu-cse-comit-client`에는 PA 툴이 없습니다. 도입 시 선택지는 두 가지입니다.

| | PostHog Cloud (무료 플랜) | PostHog Self-hosted |
|---|---|---|
| 인프라 추가 | 없음 | 컨테이너 추가 |
| 데이터 위치 | PostHog 서버 (해외) | sidowi 내부 |
| 학생 행동 데이터 국외 이전 | O | X |
| 운영 부담 | 없음 | 직접 관리 |

**Self-hosted를 선택하는 이유**

Comit은 학교 SSO로 로그인합니다. `user_id`가 붙은 행동 데이터를 해외 서버로 보내는 것은 개인정보보호법상 국외 이전에 해당하며, 이용자 동의 또는 개인정보처리방침 고지가 필요합니다.

서버 리소스를 확인한 결과 메모리 37GB, 디스크 43GB가 여유 있어 self-hosted 운영이 가능합니다. 법적으로 깔끔하고 리소스도 되는 상황에서 Cloud를 선택할 이유가 없습니다.

---

## 범위: 이번에 하지 않을 것

❌ access log 직접 구현 — Pinpoint가 처리  
❌ trace_id 직접 구현 — Pinpoint가 처리  
❌ Zipkin / Jaeger 도입 — Pinpoint로 충분  
❌ Kafka 애플리케이션 레벨 도입 — 현재 Pinpoint 전용으로만 사용 중  
❌ DW / Data Mart  
❌ 실시간 이벤트 파이프라인

---

## 보안 원칙

저장하지 않습니다:
- 비밀번호, 토큰, 쿠키 원문
- 학번 원문, 이메일 원문
- 게시글/댓글 본문 전문

allowlist 기반으로 저장하고, 민감정보는 마스킹합니다.

---

## 정리하면

| 항목 | 현황 | 할 일 |
|---|---|---|
| 요청 추적 (trace_id, access log) | ✅ Pinpoint | 없음 |
| 서비스 간 흐름 시각화 | ✅ Pinpoint Web UI | 없음 |
| Business Event 로깅 | ❌ 없음 | 직접 구현 |
| 프론트 행동 분석 | ❌ 없음 | PostHog Self-hosted 도입 |

Pinpoint가 서버 뒤를 보고, PA 툴이 서버 앞을 봅니다.  
이 두 가지가 갖춰져야 **학생이 들어와서 떠나기까지의 전체 흐름**이 보입니다.

의견 부탁드립니다.