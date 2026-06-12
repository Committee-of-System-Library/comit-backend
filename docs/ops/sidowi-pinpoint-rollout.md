# Sidowi Pinpoint 롤아웃 계획

`sidowi`에 `Pinpoint`를 도입하기 위한 운영 계획.
이 문서는 "지금 바로 production 전체에 상시 적용"이 아니라, `sidowi` 기준의 안전한 첫 도입과 확장 순서를 정리한다.

## 왜 지금 정리하는가

- `Comit`는 Java/Spring 기반 백엔드이고, 서비스가 분리될수록 호출 흐름과 병목 지점을 한눈에 보는 도구가 필요해진다.
- 현재 repo에도 observability 필요성이 이미 드러나 있다.
- 다만 `sidowi`는 이미 backend runtime, nginx, self-hosted runner를 함께 맡고 있으므로, APM 도입은 배포 스택과 관측 스택을 분리한 상태로 들어가야 한다.

## 현재 반영 상태 (`2026-04-10`)

이미 실제 서버에 반영된 항목:

- `Pinpoint 3.0.5` base stack 기동
- `Kafka + Pinot` metric stack 추가
- `Comit` backend agent 연결
- auth server agent 연결
- `Servermap`, `Inspector` 동작 확인
- `Pinpoint` 주요 metric 서비스 1차 메모리 튜닝 적용

아직 남아 있는 항목:

- `pinot-server-0`까지 포함한 2차 메모리 튜닝
- Kafka topic partition 축소 여부 판단
- `Pinpoint` UI를 path 기반 공개에서 별도 vhost로 분리할지 결정
- 만료된 `*.knu.ac.kr` TLS 인증서 교체

실제 적용 중 겪은 문제와 해결 과정은 [sidowi-pinpoint-implementation-log.md](./sidowi-pinpoint-implementation-log.md)에 따로 남긴다.

## 현재 Comit 런타임 전제

- Spring Boot: `4.0.4`
- Gradle toolchain Java: `21`
- Docker 런타임 이미지: `amazoncorretto:21`
- 배포 방식:
  - GitHub Actions build/push
  - `sidowi` self-hosted runner가 GHCR 이미지 pull
  - `/opt/docker/compose/docker-compose.services.yml` 기준으로 backend 재기동
- 운영 source of truth:
  - compose: `/opt/docker/compose/docker-compose.services.yml`
  - backend env: `/opt/docker/env/comit.env`
  - nginx conf: `/opt/docker/nginx/conf.d/*.conf`

근거:
- [build.gradle](../../build.gradle)
- [Dockerfile](../../Dockerfile)
- [deploy.yml](../../.github/workflows/deploy.yml)
- [comit-prod-like-backend-rollout.md](./comit-prod-like-backend-rollout.md)
- [backend-self-hosted-runner-flow.html](./backend-self-hosted-runner-flow.html)

## 권장 Pinpoint 버전

### 결론

- 권장 버전: `Pinpoint 3.0.5`
- 도입 조합:
  - `Comit app`: Java `21`
  - `Pinpoint agent`: `3.0.5`
  - `Pinpoint collector/web`: `3.0.5`, JVM은 Java `17`

### 이유

- 공식 최신 안정 릴리스가 `v3.0.5`다.
- 공식 호환표 기준으로 `3.0.x agent`는 Java `8-21`을 지원하므로, 현재 Comit의 Java `21`과 직접 맞는다.
- `3.1.x`를 바로 올리기보다, 첫 도입은 현재 런타임에 가장 직접적으로 맞는 `3.0.x`를 minor 통일로 가져가는 쪽이 안전하다.

## 내려받아야 하는 컴포넌트

최소 구성:

- Agent
  - [`pinpoint-agent-3.0.5.tar.gz`](https://github.com/pinpoint-apm/pinpoint/releases/download/v3.0.5/pinpoint-agent-3.0.5.tar.gz)
- Collector
  - [`pinpoint-collector-3.0.5-exec.jar`](https://github.com/pinpoint-apm/pinpoint/releases/download/v3.0.5/pinpoint-collector-3.0.5-exec.jar)
- Web
  - [`pinpoint-web-3.0.5-exec.jar`](https://github.com/pinpoint-apm/pinpoint/releases/download/v3.0.5/pinpoint-web-3.0.5-exec.jar)

선택 구성:

- Batch
  - [`pinpoint-batch-3.0.5-exec.jar`](https://github.com/pinpoint-apm/pinpoint/releases/download/v3.0.5/pinpoint-batch-3.0.5-exec.jar)
- Starter 형태
  - `pinpoint-collector-starter-3.0.5-exec.jar`
  - `pinpoint-web-starter-3.0.5-exec.jar`

## 저장소 및 주변 인프라 전제

Pinpoint `3.x`는 agent/web/collector만 보고 끝내면 안 된다.

- `HBase 2.x` 전제
- `Pinot` 전제
- 필요 시 `Kafka`, `Zookeeper`도 같이 본다

실무 해석:

- `Pinpoint 3.x`를 실제로 쓰려면 `HBase 2.x + Pinot`를 함께 예산에 넣는 것이 안전하다.
- 특히 Inspector/Metric 계열 기능까지 쓸 계획이면 Pinot는 사실상 운영 필수로 본다.
- 첫 도입은 `pinpoint-docker`를 참고해 빠르게 PoC를 띄우되, 운영 배치 자체는 `Comit` backend와 분리한다.

참고:
- [Pinpoint README](https://github.com/pinpoint-apm/pinpoint)
- [Pinpoint 3.0.5 Release](https://github.com/pinpoint-apm/pinpoint/releases/tag/v3.0.5)
- [Pinpoint Installation](https://pinpoint-apm.github.io/pinpoint/installation.html)
- [Pinpoint Docker](https://github.com/pinpoint-apm/pinpoint-docker)
- 복붙용 초안:
  - [docker-compose.pinpoint.example.yml](./pinpoint/docker-compose.pinpoint.example.yml)
  - [pinpoint.env.example](./pinpoint/pinpoint.env.example)
  - [comit.env.pinpoint.example](./pinpoint/comit.env.pinpoint.example)

## 호환성 메모

좋은 점:

- 현재 Comit 스택은 Pinpoint 공식 지원 범주와 비교적 잘 맞는다.
- Spring Boot Web, JPA, MySQL, HikariCP 조합은 Pinpoint가 지원하는 일반적인 축이다.

주의점:

- 공식 자료에서 `Spring Boot 4`를 명시적으로 적은 호환 문구는 확인되지 않았다.
- Java `21` 자체는 지원 범위 안이지만, `Spring Boot 4.0.4`는 반드시 staging에서 smoke test 후 production으로 확대해야 한다.

첫 검증에서 반드시 확인할 것:

- fat jar / container 환경에서 agent 부착 후 앱 정상 기동
- Spring MVC trace 수집
- JPA + Hikari + MySQL 쿼리 trace 수집
- 외부 HTTP 호출이 있다면 client trace 수집

## 제안 토폴로지 (`sidowi` 기준)

### 핵심 원칙

- `Comit` 앱 배포 스택과 `Pinpoint` 관측 스택은 같은 호스트에 두되, **운영 단위는 분리**한다.
- backend 앱은 기존 deploy 흐름을 유지한다.
- Pinpoint 서버 컴포넌트는 기존 backend compose에 섞지 않는다.

### 그대로 유지할 것

- `knu-cse-comit-server`는 기존처럼 `/opt/docker/compose/docker-compose.services.yml`에서 관리
- backend 배포는 기존 self-hosted runner + `deploy.yml` 유지
- backend env source of truth는 `/opt/docker/env/comit.env` 유지

### 새로 분리할 것

- Pinpoint compose: `/opt/docker/compose/docker-compose.pinpoint.yml`
- Pinpoint env: `/opt/docker/env/pinpoint.env`
- 관측용 nginx route: 필요 시 별도 conf 또는 별도 location block

### 네트워크 원칙

- backend 앱은 기존 `cse-backend` 네트워크 유지
- agent는 collector로만 송신
- collector는 외부 공개하지 않음
- web UI는 필요 시 nginx 뒤에서 제한적으로 공개
- storage 계층은 외부 공개하지 않음

### 권장 경로

| 구분 | 경로 | 역할 |
|---|---|---|
| backend compose | `/opt/docker/compose/docker-compose.services.yml` | `knu-cse-comit-server` 유지, agent mount만 추가 |
| backend env | `/opt/docker/env/comit.env` | `JAVA_OPTS`, profile, collector 주소 등 backend 주입값 |
| pinpoint compose | `/opt/docker/compose/docker-compose.pinpoint.yml` | collector/web/storage 전용 |
| pinpoint env | `/opt/docker/env/pinpoint.env` | Pinpoint 서버 설정 전용 |
| nginx | `/opt/docker/nginx/conf.d/*.conf` | Pinpoint web UI 제한 노출 |
| runner | `/home/yujihun20251/actions-runner/comit-backend` | 초기에는 backend만 재배포 |

## Pinpoint Agent 주입 방식

### 현재 가능한 이유

현재 컨테이너 시작 명령은 아래 형태다.

```sh
java ${JAVA_OPTS} -Duser.timezone=GMT+9 -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar
```

즉 애플리케이션 코드 수정 없이도 agent는 `JAVA_OPTS`로 주입 가능하다.

근거:
- [Dockerfile](../../Dockerfile)

### 권장 방식

초기 도입은 agent를 이미지에 bake하지 않고, 서버에 agent 디렉터리를 두고 volume mount 하는 방식을 권장한다.

이유:

- rollback이 빠르다
- 앱 이미지 재빌드 없이 agent on/off 가능
- 현재 deploy 흐름을 거의 건드리지 않는다

권장 경로 예시:

- agent home: `/opt/pinpoint-agent`
- applicationName: `comit-backend`
- agentId: `comit-backend-sidowi-01`

권장 env 예시:

```bash
SPRING_PROFILES_ACTIVE=staging
JAVA_OPTS=-javaagent:/opt/pinpoint-agent/pinpoint-bootstrap-3.0.5.jar \
  -Dpinpoint.agentId=comit-backend-sidowi-01 \
  -Dpinpoint.applicationName=comit-backend \
  -Dpinpoint.container
```

collector 주소가 기본값이 아니면 agent 설정 파일 또는 JVM 옵션으로 collector host/port를 맞춘다.

## 병렬 작업 스트림

이 rollout은 아래 세 스트림으로 병렬 진행하는 것이 가장 안전하다.

### Stream 1. Pinpoint 버전/저장소 준비

목표:

- `3.0.5` 기준 바이너리/이미지 버전 고정
- `HBase 2.x`, `Pinot`, 필요 시 `Kafka/Zookeeper` 구성 확정
- `pinpoint-docker` 기반 PoC 초안 확보

산출물:

- `pinpoint` 버전 매트릭스
- 다운로드 링크
- 저장소 구성 체크리스트

### Stream 2. sidowi 인프라 배치

목표:

- backend와 Pinpoint를 서로 다른 compose/env로 분리
- nginx 노출 정책과 운영 책임 분리
- self-hosted runner가 backend만 맡도록 유지

산출물:

- `/opt/docker/compose/docker-compose.pinpoint.yml`
- `/opt/docker/env/pinpoint.env`
- Pinpoint web UI 노출 정책

### Stream 3. backend agent 연결

목표:

- backend 서비스에 agent mount + `JAVA_OPTS` 주입
- auth server에도 같은 collector를 향하도록 agent를 주입
- `sidowi` staging에서 안전하게 smoke test
- 빠른 rollback 절차 확보

산출물:

- `/opt/docker/compose/docker-compose.services.yml` 변경점
- `/opt/docker/env/comit.env`, `/opt/docker/env/sso.env` 변경점
- smoke test / rollback runbook

## 권장 롤아웃 순서

1. Pinpoint 전용 compose/env 경로를 먼저 만든다.
2. Pinpoint 서버 컴포넌트를 backend와 분리해 먼저 띄운다.
3. collector/web/storage가 정상인지 확인한다.
4. web UI는 내부 접근 또는 제한된 nginx 경로로만 연다.
5. backend 서비스에 agent를 주입한다.
6. backend compose만 재기동한다.
7. trace 수집과 앱 동작을 함께 검증한다.
8. auth server까지 collector에 붙는지 확인한다.
9. 안정화 전까지는 Pinpoint 운영을 수동 runbook으로 둔다.

## Smoke Test

이 repo에는 actuator health endpoint가 없으므로 기존 공개 엔드포인트 기준으로 본다.

### 1차 부팅 확인

- `docker ps`에서 `knu-cse-comit-server`가 재기동 후 유지되는지 확인
- `docker logs knu-cse-comit-server --tail 200`에서
  - Pinpoint agent 초기화 로그
  - Spring Boot started 로그
  를 함께 확인

### auth server 연결 확인

- `docker ps`에서 `knu-cse-auth-server`가 재기동 후 유지되는지 확인
- `docker logs knu-cse-auth-server --tail 200`에서
  - Pinpoint agent 초기화 로그
  - auth server started 로그
  를 함께 확인
- `pinpoint-collector` 로그에서 아래 식별값으로 agent 연결이 잡히는지 확인한다
  - `applicationname=cse-auth-server`
  - `agentid=auth-prod-01`

### 2차 정적 리소스 확인

- `GET https://chcse.knu.ac.kr/comit-staging/api/docs`
  - 기대 결과: `302 -> /api/docs/index.html`
- `GET https://chcse.knu.ac.kr/comit-staging/api/docs/index.js`
  - 기대 결과: `200`

### 3차 DB 경유 API 확인

- `GET https://chcse.knu.ac.kr/comit-staging/api/posts/search?keyword=test`
  - 기대 결과: `200`
  - 빈 배열이어도 괜찮다

### 4차 SSO 경로 확인

staging에서 SSO를 실제로 켜 둔 경우:

- `GET /auth/sso/login`
  - 기대 결과: auth-server login URL로 `302`

### 5차 Pinpoint UI 확인

- `comit-backend` applicationName 표시 확인
- `comit-backend-sidowi-01` agent 연결 상태 확인
- `posts/search` 호출 직후 trace 수집 확인

## Rollback

초기 rollout은 앱 코드 rollback보다 **agent 비활성화 rollback**이 먼저다.

### 가장 빠른 rollback

1. `/opt/docker/env/comit.env`에서 Pinpoint 관련 `JAVA_OPTS` 제거
2. 필요 시 compose의 agent mount 제거
3. 아래 명령으로 backend만 재기동

```bash
docker compose -f /opt/docker/compose/docker-compose.services.yml up -d knu-cse-comit-server
```

4. 아래 재확인
  - container running
  - `/api/docs` 302
  - `/posts/search?keyword=test` 200

### 이미지 단위 rollback

agent를 이미지에 bake한 뒤 문제가 생기면:

- GHCR의 직전 timestamp tag 이미지로 되돌린다
- compose image tag를 이전 값으로 맞춘 뒤 재기동한다

## 배포 전 체크리스트

- [ ] Pinpoint 버전을 `3.0.5`로 고정했다
- [ ] agent는 Java 21, collector/web는 Java 17 기준으로 맞췄다
- [ ] sidowi에 `/opt/pinpoint-agent`가 준비됐다
- [ ] `/opt/docker/env/comit.env`에 `JAVA_OPTS`가 반영됐다
- [ ] `/opt/docker/compose/docker-compose.services.yml`에 agent mount가 반영됐다
- [ ] `docker logs`에서 agent init 로그가 보인다
- [ ] `/api/docs`와 `/posts/search` smoke test가 통과한다
- [ ] Pinpoint UI에서 `comit-backend` trace가 보인다
- [ ] `JAVA_OPTS` 제거만으로 되돌릴 수 있는 rollback을 확인했다

## 지금 보류할 것

- Pinpoint까지 GitHub Actions 자동 배포에 묶기
- backend compose 안에 collector/web/storage를 함께 섞기
- production 전체에 곧바로 확대 적용
- Spring Boot 4 호환 검증 없이 운영 전체 적용
