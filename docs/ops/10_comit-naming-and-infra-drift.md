# COMIT 명칭 혼재 및 인프라 드리프트 현황 분석

> 작성일: 2026-06-04  
> 목적: 서비스/레포/컨테이너/URL 명칭 불일치 원인 분석 및 교통정리 방향 제시

---

## 한 줄 요약

**comit이 core-infra shared compose에서 독립 분리(`/0_services/comit/`)되며 `comit-backend`로 이름이 바뀌었는데, `core-infra` 레포와 그 서버 체크아웃(`/opt/docker/compose/`)이 갱신되지 않아 두 개의 진실이 공존하고 있다.**

나머지 서비스(auth/ledger/locker)는 여전히 `core-infra`가 관리하는 `knu-cse-*` 명칭을 쓴다.

---

## 명칭 혼재 전체 지도

### 1. 운영 COMIT 백엔드

| 축 | 현재 값 | 출처 |
|---|---|---|
| GitHub 레포 | `comit-backend` | 구 `knu-cse-comit-server`에서 개명 |
| GHCR 이미지 | `ghcr.io/…/comit-backend:latest` | 레포 개명 후 CI 빌드명 변경 |
| 컨테이너명 | `comit-backend` | `/0_services/comit/.comit-docker-compose.yml` |
| nginx upstream | `comit-backend` | `/00_maintenance/0_proxy/conf.d/upstream.conf` |
| 외부 URL 경로 | `/comit-staging/api/` | nginx comit.conf |
| **core-infra 레포 기준** | `knu-cse-comit-server` (구 이름) | `core-infra/compose/docker-compose.services.yml` |
| **core-infra nginx 기준** | `knu-cse-comit-server:8080` upstream | `core-infra/nginx/conf.d/comit.conf` |

### 2. 부하 테스트(Load Test) 환경

| 축 | 현재 값 |
|---|---|
| GitHub 레포 | `comit-perf` |
| GHCR 이미지 | `ghcr.io/…/comit-test:latest` |
| 컨테이너명 | `comit-loadtest` |
| 외부 URL 경로 | `/comit-loadtest/api/` |
| compose 파일 | `/opt/docker/compose/docker-compose.test.yml` |

→ 하나의 환경에 4가지 다른 이름이 붙어 있다.

### 3. 서버 시각 엔드포인트 (이중 구조)

| 방식 | URL | 출처 |
|---|---|---|
| nginx 직접 응답 | `GET /comit-staging/time` | `comit.conf` `return 200` 블록 |
| 백엔드 경유 | `GET /comit-staging/api/time` | Spring `TimeController` |

두 엔드포인트가 동시에 살아 있다. nginx 직접 응답은 `$msec`(초 단위 실수)을 반환하는 반면 백엔드는 `epoch` ms 정수를 반환해 스펙이 다르다. **nginx 직접 응답 블록은 잉여이므로 제거 대상이다.**

---

## 근본 원인

### comit의 core-infra 분리 이력

```
core-infra (원본)
  └─ compose/docker-compose.services.yml
        ├─ knu-cse-auth-server   ← 아직 core-infra 관리
        ├─ knu-cse-ledger-server ← 아직 core-infra 관리
        ├─ knu-cse-locker-server ← 아직 core-infra 관리
        └─ knu-cse-comit-server  ← 여기서 분리됨 (이미 stale)

실제 comit 관리 위치
  /0_services/comit/.comit-docker-compose.yml
        └─ comit-backend         ← 레포명 변경 후 이 이름으로 운영 중
```

**org 전체 컨벤션은 `{service}-backend/-frontend`** (auth-backend, ledger-backend, locker-backend, official-backend 모두 동일). `comit-backend`로의 개명은 이 컨벤션을 따른 올바른 결정이었다.

### `/comit-staging/` 경로명이 "staging"인 이유 (추정)

comit이 처음 추가될 당시 `chcse.knu.ac.kr`의 메인 서비스는 `official-*`(홈페이지)였고, comit은 그 아래 "sub-path"로 배치됐다. "staging"은 개발 단계를 의미했을 가능성이 높다. **현재는 사실상 운영 경로이지만 URL이 바뀌지 않아 혼란을 준다.** ← 팀 확인 필요.

---

## ⚠️ 즉각 주의 필요: 라이브 지뢰

`/opt/docker/compose/docker-compose.services.yml`(core-infra 체크아웃)에 여전히 아래 내용이 있다:

```yaml
knu-cse-comit-server:
  image: ghcr.io/…/knu-cse-comit-server:latest
  container_name: knu-cse-comit-server
```

이 파일로 `docker compose up`을 실행하면 **구 이미지(`knu-cse-comit-server:latest`)를 별도 컨테이너로 기동시키고, upstream에 등록된 `comit-backend`가 아닌 `knu-cse-comit-server` 컨테이너를 바라보게 된다.** 두 백엔드가 동시에 뜨거나, nginx가 라우팅을 잃을 수 있다.

**팀원이 core-infra 기준으로 "서비스 재기동"을 시도하면 장애가 발생할 수 있다.**

---

## 교통정리 방향

변경 비용 기준으로 3단계로 나눈다.

### 즉시 가능 — 저비용

| 작업 | 대상 파일 | 내용 |
|---|---|---|
| core-infra comit 항목 동기화 | `core-infra/compose/docker-compose.services.yml` | `knu-cse-comit-server` → `comit-backend`, 이미지 경로 수정 |
| core-infra nginx 동기화 | `core-infra/nginx/conf.d/comit.conf` | upstream `knu-cse-comit-server:8080` → `comit-backend:8080` |
| nginx 이중 time 블록 제거 | `/00_maintenance/0_proxy/conf.d/comit.conf` | `/comit-staging/time` nginx 직접 응답 블록 삭제 (백엔드 `/api/time`으로 통일) |
| load-test 명칭 문서화 | `docs/ops/10_*.md` | comit-perf / comit-test / comit-loadtest 관계 명문화 |

### 팀 결정 필요 — 인프라 관리 방식 선택

**core-infra를 진실의 원본으로 유지할 것인가, 아니면 comit은 독립 관리로 공식화할 것인가.**

**옵션 A — GitOps: core-infra를 단일 원본으로**
- comit도 core-infra `compose/docker-compose.services.yml`로 복귀
- `/0_services/comit/.comit-docker-compose.yml`은 core-infra 내용을 따름
- 모든 서비스가 동일한 파이프라인 하에 관리됨
- 단점: comit-backend는 AI 기능으로 env 항목이 많아 shared compose 관리 부담 증가

**옵션 B — 독립 관리 공식화**
- core-infra에서 comit 항목을 `[Deprecated]` 처리 또는 삭제
- `/0_services/comit/`이 공식 관리 위치임을 README에 명시
- 단점: 서비스마다 관리 방식이 달라 일관성 부재

→ **현재 실정은 옵션 B에 가깝다.** 명시적으로 결정하지 않으면 지뢰(위 주의 사항) 위험이 지속된다.

### 고비용 / 브레이킹 — 신중히 검토

**`/comit-staging/` URL 경로 변경** (예: `/comit/`로 변경)

영향 범위:
- 프론트엔드 `VITE_API_BASE_URL` 수정 및 재배포
- SSO `cookie-path`, `allowed-redirect-uris` 수정
- nginx comit.conf 전면 수정
- 기존 북마크/링크 전면 무효화
- Pinpoint 로그 경로 변경

→ **실행 비용이 매우 높다. URL 혼란은 팀 내부 문서·컨벤션으로 해소하고, 실제 경로 변경은 서비스 대규모 개편 시점과 맞추는 것을 권장한다.**

---

## 현재 실질 역할 정리 (팀 공용 레퍼런스)

| URL 접두사 | 실제 역할 | 이미지 | 관리 compose |
|---|---|---|---|
| `/comit-staging/api/` | **운영 COMIT 백엔드** | `comit-backend:latest` | `/0_services/comit/.comit-docker-compose.yml` |
| `/comit-staging/` (non-api) | 운영 COMIT 프론트엔드 | `comit-frontend:latest` | 동일 |
| `/comit-loadtest/api/` | **부하 테스트 환경** | `comit-test:latest` | `/opt/docker/compose/docker-compose.test.yml` |
| `/comit-staging/time` | ~~nginx 직접 시각 응답~~ **제거 예정** | — | nginx comit.conf |
| `/comit-staging/api/time` | 운영 서버 시각 API | `comit-backend:latest` | — |

---

## 참고: 관련 파일 위치

| 파일 | 경로 |
|---|---|
| 실제 운영 compose | `sidowi:/0_services/comit/.comit-docker-compose.yml` |
| 실제 운영 env | `sidowi:/0_services/comit/.comit-prod.env` |
| core-infra repo compose | `core-infra/compose/docker-compose.services.yml` |
| core-infra nginx comit.conf | `core-infra/nginx/conf.d/comit.conf` |
| 라이브 nginx comit.conf | `sidowi:/00_maintenance/0_proxy/conf.d/comit.conf` |
| 라이브 nginx upstream | `sidowi:/00_maintenance/0_proxy/conf.d/upstream.conf` |
