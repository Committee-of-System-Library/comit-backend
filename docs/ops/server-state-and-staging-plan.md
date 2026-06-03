# 서버 상태 파악 및 스테이징 구성 계획

> 조사일시: 2026-05-31  
> 서버: `155.230.29.100:2803`  
> **서버 내 파일 수정·생성·삭제 없음 (읽기 전용 조사)**

---

## 1. 현재 서버 구조 요약

### 1-1. 실행 중인 컨테이너 (관련 서비스만)

| 컨테이너명 | 이미지 | 상태 | 노출 포트 |
|---|---|---|---|
| `comit-backend` | `ghcr.io/committee-of-system-library/comit-backend:latest` | Up 13일 (healthy) | 8080/tcp |
| `comit-frontend` | `ghcr.io/committee-of-system-library/comit-frontend:latest` | Up 4일 | 80/tcp |
| `proxy` | `nginx:latest` | Up 13일 | 80, 443, 2518 |
| `cse_DB` | `mysql:latest` | Up 13일 (healthy) | 3306/tcp |

> **⚠️ 핵심 발견**: `comit-backend`는 `SPRING_PROFILES_ACTIVE=staging`으로 실행 중이다.  
> 즉 현재 서버에 "프로덕션 전용 컨테이너"는 없고, **이 컨테이너 자체가 스테이징+운영을 겸하고 있다.**

### 1-2. Docker 네트워크

| 네트워크 | 연결 컨테이너 |
|---|---|
| `serv_comit` | `proxy`, `comit-backend`, `comit-frontend` |
| `serv_auth` | `proxy`, `auth-backend`, `auth-frontend`, `auth-keycloak` |
| `serv_DB` | `cse_DB`, `comit-backend` 등 |

> **⚠️ `docker.md`와 실제가 다름**: `docker.md`에 기재된 `cse-proxy`/`cse-backend` 네트워크는 존재하지 않는다.  
> 실제 네트워크는 `serv_comit`이다. `docker-compose.staging.yml`의 네트워크 항목을 수정해야 한다.

### 1-3. comit-backend 환경변수 (민감정보 제외)

```
SPRING_PROFILES_ACTIVE=staging
SPRING_PORT=8080
DB_URL=jdbc:mysql://cse-db:3306/comit_staging?...
VIRTUAL_THREADS_ENABLED=true
COMIT_AUTH_SSO_ENABLED=true
COMIT_AUTH_BRIDGE_ENABLED=false
LOG_FILE_PATH=/app/logs/app-staging.log
JAVA_OPTS=-javaagent:/opt/pinpoint-agent/...  # Pinpoint APM 에이전트 연결됨
```

---

## 2. nginx 구성 분석

### 2-1. 파일 구조

```
/opt/docker/nginx/
├── nginx.conf                    # worker, resolver(127.0.0.11), http 설정
├── conf.d/
│   ├── chcse.knu.ac.kr.conf      # 443 메인 서버블록, conf.d/*.conf include
│   ├── comit.conf                # comit 관련 location 블록
│   ├── appfn.conf                # SSO 관련
│   ├── ledger.conf
│   ├── locker.conf
│   ├── common-headers.conf
│   └── .htpasswd-comit-staging   # Basic Auth 파일 (이미 존재)
├── ssl/
└── 0_certification/              # 인증서 (*.knu.ac.kr wildcard)
```

### 2-2. comit.conf 현재 라우팅

```
/comit-staging/api/       → http://knu-cse-comit-server:8080/
/comit-staging/pinpoint/  → http://pinpoint-web:8080 (pinpoint APM UI)
```

> **핵심**: nginx는 `knu-cse-comit-server`라는 DNS 이름으로 백엔드를 찾는다.  
> Docker Compose의 **서비스 이름(service key)** 이 `serv_comit` 네트워크에서 DNS가 된다.  
> 현재 `comit-backend` 컨테이너의 compose 서비스 이름이 `knu-cse-comit-server`이기 때문에  
> nginx → `knu-cse-comit-server:8080` → `comit-backend` 컨테이너로 정상 라우팅되고 있다.

### 2-3. .htpasswd-comit-staging

`conf.d/.htpasswd-comit-staging` 파일이 존재하지만, 현재 `comit.conf`에서 **참조되지 않는다.**  
즉 Basic Auth는 설정되어 있지 않은 상태다. (과거 준비했으나 미적용)

---

## 3. /opt/docker/env 파일 현황

| 파일 | 설명 |
|---|---|
| `comit.env` | 현재 `comit-backend` 컨테이너에 적용 중 |
| `comit-staging.env` | **존재하지 않음** → 로드 테스트용 컨테이너에 필요 |
| `sso.env` | SSO/Keycloak 설정 |
| `ledger.env`, `locker.env` | 각 서비스 |

---

## 4. docker.md와 실제 서버의 차이점

| 항목 | docker.md 기술 | 실제 서버 |
|---|---|---|
| 네트워크 이름 | `cse-proxy`, `cse-backend` | `serv_comit`, `serv_auth` 등 |
| comit 컨테이너명 | `knu-cse-comit-server` | `comit-backend` |
| comit 이미지 | `knu-cse-comit-server:latest` | `comit-backend:latest` |
| Keycloak 컨테이너명 | `knu-cse-keycloak` | `auth-keycloak` |
| auth 서버명 | `knu-cse-auth-server` | `auth-backend` |

> `docker.md`는 현재 서버 상태와 맞지 않는다. 내용 갱신이 필요하다.

---

## 5. 부하 테스트용 스테이징 구성 계획

### 5-1. 현재 구조의 제약

현재 `comit-backend` 하나가 운영과 스테이징을 겸하기 때문에,  
로드 테스트를 이 컨테이너에서 실행하면 **실제 사용자 트래픽에 영향**을 줄 수 있다.  
→ 로드 테스트 전용 **별도 컨테이너**(`comit-loadtest`)를 띄우는 것을 권장한다.

### 5-2. 권장 구성 (컨테이너 분리)

```
현재:
  nginx → knu-cse-comit-server:8080 → comit-backend (운영)

추가:
  nginx → comit-loadtest:8080 → comit-loadtest (부하 테스트 전용)
```

**추가해야 할 것들:**

#### (A) `/opt/docker/env/comit-loadtest.env` 생성 (서버에서 작업 필요)

`comit.env` 기반으로 복사 후 아래만 수정:

```env
# comit.env를 복사하고 아래 항목만 변경
SPRING_PROFILES_ACTIVE=staging
COMIT_LOAD_TEST_ENABLED=true        # ← Mock API 활성화 (핵심)
COMIT_DEV_AUTH_ENABLED=true         # ← DevAuth 활성화 (선택)
COMIT_AUTH_SSO_ENABLED=false        # ← 부하 테스트 중 SSO 불필요

# Pinpoint agentId는 중복 불가 → 별도 지정
JAVA_OPTS=-javaagent:/opt/pinpoint-agent/... -Dpinpoint.agentId=comit-loadtest-01
```

#### (B) nginx `comit.conf`에 location 추가 (서버에서 작업 필요)

```nginx
location ^~ /comit-loadtest/api/ {
    auth_basic           "Load Test";
    auth_basic_user_file /etc/nginx/custom/conf.d/.htpasswd-comit-staging;
    proxy_pass           http://comit-loadtest:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Prefix /comit-loadtest/api;
}
```

> `.htpasswd-comit-staging`이 이미 존재하므로 Basic Auth를 바로 적용할 수 있다.  
> K6 요청 시 `--http-debug` 또는 헤더에 `Authorization: Basic ...` 추가 필요.

#### (C) `docker-compose.staging.yml` 수정 사항

현재 내 파일의 문제점과 수정 방향:

| 항목 | 현재 파일 | 수정 필요 내용 |
|---|---|---|
| 서비스/컨테이너명 | `comit-staging` | `comit-loadtest` |
| 네트워크 | `cse-proxy`, `cse-backend` (❌ 없음) | `serv_comit` |
| env_file 경로 | `/opt/docker/env/comit-staging.env` | `/opt/docker/env/comit-loadtest.env` |
| 이미지 | `${ORG_LC}/${IMAGE_NAME}:staging` | `ghcr.io/committee-of-system-library/comit-backend:staging` |

#### (D) GitHub Actions `deploy-staging.yml` 수정 사항

| 항목 | 현재 값 | 수정 필요 값 |
|---|---|---|
| `CONT_NAME` | `comit-staging` | `comit-loadtest` |
| 이미지 태그 push | `:staging` | `:staging` (유지) |

### 5-3. 가장 빠른 대안 (컨테이너 추가 없이)

로드 테스트 기간이 짧다면, 기존 `comit-backend`에 임시로 적용하는 방법도 있다.

```
comit.env에 COMIT_LOAD_TEST_ENABLED=true 추가
→ docker compose restart comit-backend (또는 update)
→ 테스트 완료 후 제거
```

단점: **운영 중인 컨테이너를 건드리므로 리스크 있음**. 격리된 `comit-loadtest` 컨테이너를 권장.

---

## 6. K6 테스트 접속 URL (구성 완료 시)

```
부하 테스트 대상: https://chcse.knu.ac.kr/comit-loadtest/api/load-test/...

K6 실행:
  ./load-test/run.sh https://chcse.knu.ac.kr/comit-loadtest/api 100 400
```

---

## 7. 서버에서 해야 할 작업 목록

| # | 작업 | 담당 | 비고 |
|---|---|---|---|
| 1 | `/opt/docker/env/comit-loadtest.env` 생성 | 서버 운영자 | `comit.env` 복사 후 수정 |
| 2 | `docker-compose.staging.yml` 서버에 배치 | 서버 운영자 | 이 저장소의 파일 사용 |
| 3 | nginx `comit.conf`에 `/comit-loadtest/api/` location 추가 | 서버 운영자 | nginx reload 필요 |
| 4 | GitHub Secrets 추가 | 저장소 관리자 | `COMIT_STAGING_DOCKER_COMPOSE_FILE_PATH`, `COMIT_STAGING_ENV_PATH` |
| 5 | `staging` 브랜치 push → GitHub Actions 배포 | 개발자 | `comit-loadtest` 컨테이너 생성 |
| 6 | K6 실행 및 Excel 보고서 확인 | 개발자 | `./load-test/run.sh` |

---

## 8. `docker.md` 갱신 필요 항목

- 네트워크 이름 `cse-proxy`/`cse-backend` → `serv_comit`/`serv_auth` 등으로 수정
- 컨테이너명 `knu-cse-comit-server` → `comit-backend` (container_name 기준)
- `knu-cse-keycloak` → `auth-keycloak`
- `knu-cse-auth-server` → `auth-backend`
