# 부하 테스트 환경(comit-loadtest) 구성 메뉴얼

> 작성일: 2026-05-31  
> 대상 서버: `155.230.29.100:2803`  
> 목적: `comit-test` 이미지를 별도 컨테이너로 띄워 Mock API를 통해 K6 스파이크 테스트 실행

---

## 0. 현재 상태 요약

| 항목 | 상태 | 비고 |
|---|---|---|
| `docker-compose.test.yml` | ⚠️ **선두 공백 버그** | 모든 줄 2칸 들여쓰기 → YAML 파싱 실패 |
| `comit.loadtest.env` | ⚠️ **선두 공백 버그** | 모든 줄 2칸 들여쓰기 → env 키 불인식 |
| env_file 이름 | ⚠️ **불일치** | 파일: `comit.loadtest.env`(dot) / compose 참조: `comit-loadtest.env`(hyphen) |
| GitHub Actions Runner (BE) | ⚠️ **inactive dead** | 서비스 등록됨, 시작만 하면 됨 |
| MySQL 8.0.36 이미지 | ❌ **미캐시** | pull 필요 |
| `comit-test:latest` 이미지 | ❌ **미캐시** | CI/CD 빌드 후 생성됨 |
| nginx `/comit-loadtest/` 블록 | ❌ **없음** | 추가 필요 |
| nginx 로그 디렉토리 | ❌ **없음** | 생성 필요 |

---

## 1. GitHub Actions Runner 시작

### 현상
- `deploy-staging.yml` 잡이 "Waiting for a runner to pick up this job..." 상태
- `actions.runner.Committee-of-System-Library-knu-cse-comit-server.Comit_BE` 서비스가 등록됐으나 `inactive dead`

### 조치
```bash
sudo systemctl start actions.runner.Committee-of-System-Library-knu-cse-comit-server.Comit_BE
sudo systemctl status actions.runner.Committee-of-System-Library-knu-cse-comit-server.Comit_BE
```

부팅 시 자동 시작이 필요하면:
```bash
sudo systemctl enable actions.runner.Committee-of-System-Library-knu-cse-comit-server.Comit_BE
```

### 확인
```bash
systemctl list-units --type=service | grep comit-be
# Active: active (running) 이어야 함
```

---

## 2. 파일 오류 수정 (선두 공백 제거 + 이름 통일)

### 2-1. docker-compose.test.yml 재작성

현재 모든 줄에 2칸 들여쓰기가 있어 **YAML 파싱 오류**가 발생합니다.  
아래 내용으로 `/opt/docker/compose/docker-compose.test.yml`을 **덮어씁니다**:

```bash
cat > /opt/docker/compose/docker-compose.test.yml << 'EOF'
networks:
  serv_comit:
    external: true
  comit-test-internal:
    driver: bridge

services:
  comit-test-db:
    image: mysql:8.0.36
    container_name: comit-test-db
    environment:
      MYSQL_ROOT_PASSWORD: comit-test
      MYSQL_DATABASE: comit_test
      MYSQL_USER: comit_test
      MYSQL_PASSWORD: comit-test
      TZ: Asia/Seoul
    networks:
      - comit-test-internal
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-pcomit-test"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
    restart: unless-stopped

  comit-loadtest:
    image: ghcr.io/committee-of-system-library/comit-test:latest
    container_name: comit-loadtest
    env_file:
      - /opt/docker/env/comit-loadtest.env
    environment:
      SPRING_PROFILES_ACTIVE: staging
      SPRING_PORT: "8080"
      COMIT_LOAD_TEST_ENABLED: "true"
      DB_URL: jdbc:mysql://comit-test-db:3306/comit_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      DB_USERNAME: comit_test
      DB_PASSWORD: comit-test
    depends_on:
      comit-test-db:
        condition: service_healthy
    networks:
      - serv_comit
      - comit-test-internal
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    restart: unless-stopped
EOF
```

### 2-2. env_file 이름 통일 (dot → hyphen)

compose가 참조하는 이름(`comit-loadtest.env`)으로 **복사** 또는 **이름 변경**:

```bash
# 기존 파일을 올바른 이름으로 복사 (원본 보존)
cp /opt/docker/env/comit.loadtest.env /opt/docker/env/comit-loadtest.env
```

복사 후 `/opt/docker/env/comit-loadtest.env`를 열어 **선두 공백 제거** 확인:

```bash
# 선두 공백이 있는지 확인
cat -A /opt/docker/env/comit-loadtest.env | head -5
# 올바른 상태: "SPRING_PROFILES_ACTIVE=staging$"
# 버그 상태:   "  SPRING_PROFILES_ACTIVE=staging$"  (앞에 공백)
```

공백이 있으면 제거:
```bash
sed -i 's/^[[:space:]]*//' /opt/docker/env/comit-loadtest.env
```

최종 파일 내용:
```env
SPRING_PROFILES_ACTIVE=staging
SPRING_PORT=8080
DDL_AUTO=none
MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=10MB
LOG_FILE_PATH=/app/logs/app-loadtest.log
VIRTUAL_THREADS_ENABLED=true

COMIT_LOAD_TEST_ENABLED=true
COMIT_DEV_AUTH_ENABLED=true
COMIT_AUTH_BRIDGE_ENABLED=false
COMIT_AUTH_SSO_ENABLED=false

COMIT_WEB_CORS_ALLOWED_ORIGINS=https://chcse.knu.ac.kr
COMIT_AUTH_ADMIN_EMAILS=wlgns12370@gmail.com,kimcrystarstal@gmail.com,kang4030@knu.ac.kr,toothless042@gmail.com

S3_BUCKET_NAME=bread-feet
S3_REGION=ap-northeast-2
S3_ACCESS_KEY=<S3_ACCESS_KEY>
S3_SECRET_KEY=<S3_SECRET_KEY>
S3_BASE_URL=https://bread-feet.s3.ap-northeast-2.amazonaws.com

JAVA_OPTS=-javaagent:/opt/pinpoint-agent/pinpoint-bootstrap.jar -Dpinpoint.applicationName=comit-loadtest -Dpinpoint.agentId=comit-loadtest-01 -Dpinpoint.container -Dprofiler.transport.module=GRPC -Dprofiler.transport.grpc.collector.ip=pinpoint-collector -Dprofiler.transport.grpc.agent.collector.ip=pinpoint-collector -Dprofiler.transport.grpc.metadata.collector.ip=pinpoint-collector -Dprofiler.transport.grpc.stat.collector.ip=pinpoint-collector -Dprofiler.transport.grpc.span.collector.ip=pinpoint-collector -Dprofiler.transport.grpc.agent.collector.port=9991 -Dprofiler.transport.grpc.metadata.collector.port=9991 -Dprofiler.transport.grpc.stat.collector.port=9992 -Dprofiler.transport.grpc.span.collector.port=9993
LOG_LEVEL_AUTH=DEBUG
```

> ⚠️ `JAVA_OPTS`는 반드시 **한 줄**이어야 합니다. 줄바꿈이 들어가면 Pinpoint 연결이 깨집니다.

---

## 3. 이미지 사전 Pull

첫 실행 시 `--pull never` 옵션 없이 실행하거나 아래 명령으로 미리 Pull해야 합니다.

```bash
# MySQL 8.0.36 (서버에 8.0.36 없음, 최신 8.0 태그 사용 가능)
docker pull mysql:8.0.36

# comit-test 이미지 (CI/CD가 먼저 push해야 존재함)
# staging 브랜치 push → GitHub Actions 빌드 완료 후:
docker pull ghcr.io/committee-of-system-library/comit-test:latest
```

> CI/CD가 아직 실패 상태면 이미지가 없으므로 컨테이너 기동 불가.  
> **순서: Runner 시작 → staging 브랜치 push → Actions 완료 → 수동 pull 또는 compose up**

---

## 4. nginx 설정 추가

### 4-1. 로그 디렉토리 생성

```bash
mkdir -p /var/log/nginx/comit_loadtest_be
```

### 4-2. `/opt/docker/nginx/conf.d/comit.conf` 에 블록 추가

기존 파일 맨 위 또는 `/comit-staging/api/` 블록 바로 앞에 아래를 추가합니다:

```nginx
location ^~ /comit-loadtest/api/ {
    # 부하 테스트 Mock API — 인증 우회 엔드포인트이므로 Basic Auth 필수
    auth_basic           "Load Test";
    auth_basic_user_file /etc/nginx/custom/conf.d/.htpasswd-comit-staging;

    proxy_pass           http://comit-loadtest:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Prefix /comit-loadtest/api;
    access_log /var/log/nginx/comit_loadtest_be/access.log;
    error_log  /var/log/nginx/comit_loadtest_be/error.log;
}
```

> **보안 필수**: `COMIT_LOAD_TEST_ENABLED=true` 상태에서 인증 없이 공개되면  
> `/load-test/setup`, `/load-test/night-snacks/{id}/apply` 등 인증 우회 API가  
> 인터넷에 노출됩니다. `.htpasswd-comit-staging`이 이미 서버에 존재하므로 그대로 사용.

### 4-3. nginx 설정 검증 및 reload

```bash
# proxy 컨테이너 안에서 nginx 설정 검증
docker exec proxy nginx -t

# 이상 없으면 reload (무중단)
docker exec proxy nginx -s reload
```

### 4-4. .htpasswd 비밀번호 확인

K6 실행 시 Basic Auth 헤더가 필요합니다. 비밀번호는:

```bash
cat /opt/docker/nginx/conf.d/.htpasswd-comit-staging
# 형식: comitstaging:$apr1$...
# 아이디: comitstaging
# 비밀번호: 별도 확인 필요 (htpasswd 생성 시 설정한 값)
```

---

## 5. 컨테이너 기동

```bash
docker compose -f /opt/docker/compose/docker-compose.test.yml up -d
```

### 상태 확인

```bash
# 컨테이너 상태
docker ps | grep -E "comit-test|comit-loadtest"

# DB 헬스체크 통과 확인 (healthy 이어야 함)
docker inspect comit-test-db --format "{{.State.Health.Status}}"

# 앱 헬스체크
curl -f http://localhost:8080/actuator/health  # 내부에서 직접 확인할 경우
# 또는 nginx 통해:
curl -u comitstaging:<PASSWORD> https://chcse.knu.ac.kr/comit-loadtest/api/actuator/health
```

---

## 6. 컨테이너 중지 (테스트 종료 후)

```bash
docker compose -f /opt/docker/compose/docker-compose.test.yml down
```

DB 볼륨까지 삭제(완전 초기화):
```bash
docker compose -f /opt/docker/compose/docker-compose.test.yml down -v
```

---

## 7. CI/CD 연동 (GitHub Actions)

GitHub Secrets에 아래 두 값 등록:

| Secret 이름 | 값 |
|---|---|
| `COMIT_STAGING_DOCKER_COMPOSE_FILE_PATH` | `/opt/docker/compose/docker-compose.test.yml` |
| `COMIT_STAGING_ENV_PATH` | `/opt/docker/env/comit-loadtest.env` |

`staging` 브랜치에 push하면 자동으로:
1. 테스트 실행
2. `ghcr.io/committee-of-system-library/comit-test:latest` 빌드 & push
3. 서버에서 `docker compose up -d comit-loadtest` 실행

---

## 8. K6 부하 테스트 실행 (로컬에서)

```bash
# 사전 준비: comit-loadtest 컨테이너 정상 기동 확인 후
mkdir -p load-test/results

k6 run \
  --out json=load-test/results/raw.json \
  -e BASE_URL=https://chcse.knu.ac.kr/comit-loadtest/api \
  -e CAPACITY=100 \
  -e VUS=400 \
  load-test/spike-test.js

# Excel 보고서 생성
python3 load-test/to-excel.py load-test/results/raw.json load-test/results/report.xlsx
```

> `BASE_URL`에 Basic Auth가 필요하면 K6 `http.post()` 요청에 `Authorization` 헤더 추가 필요.

---

## 9. 전체 작업 순서 요약

```
1. Runner 시작
   sudo systemctl start actions.runner.Committee-of-System-Library-knu-cse-comit-server.Comit_BE

2. 파일 수정 (선두 공백 제거, 이름 통일)
   - docker-compose.test.yml 재작성 (섹션 2-1)
   - comit-loadtest.env 생성 (섹션 2-2)

3. staging 브랜치 push → CI/CD 완료 대기 (comit-test:latest 이미지 생성)

4. 이미지 pull (필요 시)
   docker pull mysql:8.0.36
   docker pull ghcr.io/committee-of-system-library/comit-test:latest

5. nginx 설정 추가 및 reload (섹션 4)
   mkdir -p /var/log/nginx/comit_loadtest_be
   # comit.conf 편집 후:
   docker exec proxy nginx -t && docker exec proxy nginx -s reload

6. 컨테이너 기동
   docker compose -f /opt/docker/compose/docker-compose.test.yml up -d

7. K6 테스트 실행 (섹션 8)

8. 테스트 종료 후 컨테이너 중지
   docker compose -f /opt/docker/compose/docker-compose.test.yml down
```
