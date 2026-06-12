# Sidowi Pinpoint 적용 기록

`sidowi`에 `Pinpoint`를 실제로 붙이면서 어떤 순서로 적용했고, 어디서 막혔고, 어떤 기준으로 결정했는지를 남기는 운영 기록.
계획 문서는 [sidowi-pinpoint-rollout.md](./sidowi-pinpoint-rollout.md), 복붙용 초안은 [pinpoint/README.md](./pinpoint/README.md)를 본다.

## 문서 범위

- 실제 `sidowi` 서버에 반영한 Pinpoint 구성
- `Comit` backend agent 주입 방식
- `Servermap`, `Inspector`를 살리는 과정에서 겪은 트러블슈팅
- 이후 재배포나 업그레이드 때 다시 따라갈 수 있는 의사결정 배경

## 적용 전제

- `Comit` backend 런타임: Java `21`, Spring Boot `4.0.4`
- Pinpoint 버전: `3.0.5`
- backend agent 식별:
  - `applicationName=comit-backend`
  - `agentId=comit-stg-01`
- 현재 공개 경로:
  - `https://chcse.knu.ac.kr/comit-staging/pinpoint/`

## 실제 적용 상태

### backend 쪽

- backend 컨테이너는 기존 `/opt/docker/compose/docker-compose.services.yml`을 유지한다.
- Pinpoint agent는 이미지에 bake하지 않고 서버 경로를 mount해서 주입한다.
- agent 설치 경로:
  - `/opt/pinpoint-agent/pinpoint-agent-3.0.5`
- backend env source of truth:
  - `/opt/docker/env/comit.env`

현재 backend 식별값:

- `applicationName=comit-backend`
- `agentId=comit-stg-01`

### SSO 서버 쪽

- auth server도 기존 `/opt/docker/compose/docker-compose.services.yml` 안에서 관리한다.
- auth server 이미지는 그대로 두고, backend와 같은 방식으로 agent를 volume mount해서 붙였다.
- auth server env source of truth:
  - `/opt/docker/env/sso.env`

현재 SSO 식별값:

- `applicationName=cse-auth-server`
- `agentId=auth-prod-01`

### Pinpoint 서버 스택

- compose:
  - `/opt/docker/compose/docker-compose.pinpoint.yml`
- env:
  - `/opt/docker/env/pinpoint.env`

기본 스택:

- `pinpoint-web`
- `pinpoint-collector`
- `pinpoint-hbase`
- `pinpoint-mysql`
- `pinpoint-redis`
- `pinpoint-zoo1`
- `pinpoint-zoo2`
- `pinpoint-zoo3`

Inspector용 metric 스택 추가 후 상태:

- `pinpoint-kafka`
- `pinpoint-kafka-init`
- `pinot-zoo`
- `pinot-controller`
- `pinot-broker-0`
- `pinot-server-0`
- `pinot-init`

추가로 `pinpoint-web`, `pinpoint-collector`는 metric 이미지로 전환했다.

## 실제 적용 순서

1. `Pinpoint 3.0.5` 기준으로 기본 스택을 `sidowi`에 띄운다.
2. `Comit` backend에 agent를 mount하고 `JAVA_OPTS`로 주입한다.
3. nginx 뒤에서 `Pinpoint UI`를 `/comit-staging/pinpoint/` 경로로 노출한다.
4. `Servermap` 확인 후 `Inspector`가 깨지는 원인을 분리한다.
5. base compose를 갈아엎지 않고 metric overlay를 추가해 `Kafka + Pinot`를 붙인다.
6. `Inspector` API가 `200`을 반환하는지 직접 확인한다.

## 트러블슈팅 기록

### 1. UI가 흰 화면만 보였다

증상:

- `https://chcse.knu.ac.kr/comit-staging/pinpoint/` 접속 시 흰 화면
- 자산 자체는 `200`이었지만 라우터와 API 호출 경로가 맞지 않았다

원인:

- Pinpoint 프론트가 `/api`, `/serverMap`, `/main` 같은 루트 경로를 전제로 동작했다
- 하지만 실제 공개 경로는 `/comit-staging/pinpoint/` 하위 경로였다

대응:

- nginx rewrite만으로 버티지 않고, 실제 서빙되는 JS 번들을 path prefix 기준으로 패치했다
- patched asset 경로:
  - `/opt/docker/nginx/conf.d/pinpoint-assets/`
- 대표 수정 포인트:
  - base path를 `/comit-staging/pinpoint`로 고정
  - API base path를 `/comit-staging/pinpoint/api`로 고정
  - router basename이 prefix를 유지하도록 보정

검증:

- Playwright로 `Servermap` 렌더링과 애플리케이션 선택까지 확인했다
- `getServerMapDataV2` 계열 API가 `200`으로 응답하는 것을 확인했다

### 2. Servermap은 보이는데 Inspector는 계속 실패했다

증상:

- `Servermap`은 정상 렌더링
- `Inspector`는 `Error Details: An error occurred while fetching the data`
- `/api/inspector/applicationStat/chart*` 요청이 `404`

판단:

- 이 시점부터는 경로 문제가 아니라 Pinpoint 내부 metric 스택 문제로 봤다
- UI 리팩토링보다 `Inspector`가 요구하는 컴포넌트를 갖추는 것이 우선이라고 판단했다

근거:

- 공식 `pinpoint-docker` 예시와 `metric` 구성 문서를 다시 확인했다
- Pinpoint `3.x`에서 Inspector는 `Kafka + Pinot` 축이 필요하다고 보고, compose를 확장하는 방향으로 결정했다

### 3. base compose를 뜯지 않고 metric overlay를 추가했다

의도:

- 이미 살아 있는 base Pinpoint 스택을 크게 흔들지 않기 위해서다
- rollback도 쉽게 하려면 base compose 위에 metric compose를 덧붙이는 방식이 유리했다

실행:

- base:
  - `/opt/docker/compose/docker-compose.pinpoint.yml`
- overlay:
  - 임시 파일 기준 `/tmp/docker-compose.pinpoint.metric.yml`

결과:

- 기존 `web/collector/hbase/mysql/redis/zookeeper`는 유지
- metric 관련 서비스만 추가 기동

### 4. pinot-init가 GitHub raw URL 404로 실패했다

증상:

- `pinot-init` 로그에 `curl: (22) The requested URL returned error: 404`

원인:

- raw GitHub 경로에 `3.0.5`를 쓰고 있었는데, 실제 태그 경로는 `v3.0.5`였다

대응:

- init 스크립트 URL을 `.../pinpoint/v${PINPOINT_VERSION}/...` 형식으로 수정했다

확인:

- 이후 `pinot-init`가 테이블 추가를 진행했고, 로그에 `successfully added`가 찍혔다

### 5. Inspector가 404에서 500으로 바뀌었다

의미:

- 경로가 안 맞던 단계는 지나갔고, 이제는 web이 실제로 Pinot 쪽 질의를 시도하고 있다는 뜻이었다
- 이건 문제의 위치가 프론트가 아니라 데이터소스 설정으로 좁혀졌다는 신호였다

### 6. Pinot JDBC만 맞추고 broker 설정을 빼먹고 있었다

증상:

- `pinpoint-web` 로그에 Pinot datasource가 뜨지만 `brokers='null'`
- Inspector 호출 시 `500`

원인:

- `jdbc-url`만 맞추면 되는 줄 알았는데, Pinpoint `3.x` metric 모듈은 broker 설정도 따로 읽는다
- 실제로는 `spring.pinot-datasource.pinot.brokers`가 필요했다

대응:

- `/opt/docker/env/pinpoint.env`에 아래 값을 추가했다
  - `SPRING_PINOTDATASOURCE_PINOT_JDBCURL=jdbc:pinot://pinot-controller:9000`
  - `SPRING_PINOTDATASOURCE_PINOT_BROKERS=pinot-broker-0:8099`
  - `SPRING_PINOTDATASOURCE_PINOT_USERNAME=admin`
  - `SPRING_PINOTDATASOURCE_PINOT_PASSWORD=admin`

확인:

- `pinpoint-web` 재기동 후 Inspector API가 `200`을 반환했다
- 실제 확인한 API 예:
  - `/comit-staging/pinpoint/api/inspector/applicationStat/chart?...&metricDefinitionId=heap`

### 7. 1차 메모리 튜닝은 heap cap과 container limit만 먼저 적용했다

배경:

- Inspector까지 붙인 뒤 `Pinpoint` 스택 전체 메모리 사용량이 크게 올라갔다
- 특히 `pinot-controller`, `pinot-broker-0`, `pinot-server-0`, `pinpoint-kafka`가 metric 스택 전체 부담의 대부분을 차지했다
- 첫 튜닝에서는 Kafka topic partition이나 Pinot schema를 다시 만지지 않고, JVM heap과 container limit만 먼저 보수적으로 줄이기로 결정했다

초기 시도와 실패:

- `pinpoint-web`을 `mem_limit=1g`로, `pinpoint-collector`를 `mem_limit=1536m`로 먼저 줄였다
- 하지만 두 컨테이너 모두 Paketo memory calculator 기준 상한이 너무 낮아 restart loop에 들어갔다
- 로그 기준으로는 다음 수준의 메모리가 필요했다
  - `pinpoint-web`: 약 `1559170K`
  - `pinpoint-collector`: 약 `1822526K`

최종 1차 적용값:

- `pinpoint-web`
  - `JAVA_TOOL_OPTIONS=-Xms256m -Xmx768m`
  - `mem_limit=2g`
- `pinpoint-collector`
  - `JAVA_TOOL_OPTIONS=-Xms512m -Xmx1024m`
  - `mem_limit=2g`
- `pinpoint-kafka`
  - `mem_limit=2g`
- `pinot-controller`
  - `JAVA_OPTS=-Xms1g -Xmx2g -Dpinot.admin.system.exit=false`
  - `mem_limit=3g`
- `pinot-broker-0`
  - `JAVA_OPTS=-Xms1g -Xmx2g -Dpinot.admin.system.exit=false`
  - `mem_limit=3g`

이번 단계에서 의도적으로 건드리지 않은 것:

- `pinot-server-0`
- `pinpoint-hbase`
- Kafka topic partition 수

이유:

- `pinot-server-0`는 실제 metric 저장 부담이 가장 크므로 1차에서 급하게 자르면 Inspector 안정성 검증이 어려워진다
- Kafka partition 축소는 topic/table 재초기화가 필요하므로, heap cap보다 뒤 단계에서 다루는 편이 안전하다

튜닝 후 확인한 주요 수치:

- `pinpoint-web`: 약 `667MiB / 2GiB`
- `pinpoint-collector`: 약 `892MiB / 2GiB`
- `pinpoint-kafka`: 약 `457MiB / 2GiB`
- `pinot-controller`: 약 `1.16GiB / 3GiB`
- `pinot-broker-0`: 약 `1.08GiB / 3GiB`

### 8. SSO 서버에도 agent를 붙여 호출 흐름을 함께 보기로 했다

배경:

- `Comit` backend만 보이면 로그인/인증 관련 trace가 중간에서 끊긴다
- 실제 운영에서는 `SSO -> Comit backend` 경로를 같이 봐야 병목과 실패 지점을 추적하기 쉽다

실행:

- `/opt/docker/compose/docker-compose.services.yml`의 `knu-cse-auth-server`에 agent mount를 추가했다
- auth server `JAVA_OPTS`에 아래 Pinpoint 설정을 넣었다
  - `-javaagent:/opt/pinpoint-agent/pinpoint-bootstrap.jar`
  - `-Dpinpoint.applicationName=cse-auth-server`
  - `-Dpinpoint.agentId=auth-prod-01`
  - `-Dpinpoint.container`

연동 중 겪은 문제:

- auth server 기동 직후에는 `UnknownHostException: pinpoint-collector`가 한 번 발생했다
- 원인은 auth server 설정이 아니라, 같은 시점에 collector가 재기동 중이라 DNS/네트워크 alias가 아직 안정화되지 않았기 때문이었다

확인:

- `https://chcse.knu.ac.kr/appfn/api/auth/me`가 `200`으로 응답했다
- collector 로그에서 아래 식별값으로 span stream 초기화가 잡혔다
  - `applicationname=cse-auth-server`
  - `agentid=auth-prod-01`

### 9. Pinpoint 적용 이후 TLS 경고는 APM이 아니라 만료된 인증서 때문이었다

증상:

- 브라우저에서 `연결이 비공개로 설정되어 있지 않습니다` 경고가 떴다

확인:

- 서버가 실제로 내보내는 인증서는 `CN=*.knu.ac.kr`였다
- 만료 시각은 `2026-04-09 23:59:59 GMT`였고, 확인 시점 서버 시각은 이미 `2026-04-10 KST`였다
- 즉 `Pinpoint` path 라우팅과 무관하게 `chcse.knu.ac.kr` 전체에 걸친 TLS 인증서 만료 문제였다

현재 nginx 참조 경로:

- 설정 파일:
  - `/opt/docker/nginx/conf.d/chcse.knu.ac.kr.conf`
- 실제 인증서 파일:
  - `/opt/docker/nginx/0_certification/star_knu_ac_kr_cert.pem`
  - `/opt/docker/nginx/0_certification/star_knu_ac_kr_nopass_key.pem`

의미:

- `Pinpoint UI`만의 장애가 아니라 `chcse.knu.ac.kr`에 묶인 다른 서비스에도 같은 경고가 발생할 수 있다
- 후속 조치는 학교/도메인 담당자에게 갱신된 `*.knu.ac.kr` 인증서를 받아 교체하는 것이다

## 의사결정 기록

### 1. backend compose와 Pinpoint compose를 분리 유지한다

이유:

- backend 배포 파이프라인을 Pinpoint 운영 이슈와 분리해야 rollback이 쉽다
- self-hosted runner는 계속 backend만 재배포하게 두는 편이 안전하다

결론:

- backend는 `/opt/docker/compose/docker-compose.services.yml`
- Pinpoint는 `/opt/docker/compose/docker-compose.pinpoint.yml`

### 2. 첫 도입은 path 기반 공개를 유지한다

이유:

- `pinpoint.chcse.knu.ac.kr` 같은 별도 서브도메인을 쓰면 더 깔끔하지만, 당시 DNS가 준비되지 않았다
- 이미 `comit-staging` 경로 체계가 살아 있어서 먼저 기능을 살리는 쪽을 택했다

대신 남긴 리스크:

- path 기반 공개는 Pinpoint 프론트 업그레이드 때 다시 손볼 가능성이 높다
- 장기적으로는 별도 vhost 또는 서브도메인으로 분리하는 것이 더 낫다

### 3. 최소 변경 원칙을 유지한다

이유:

- base Pinpoint 스택이 이미 살아 있는 상황에서 모든 compose를 다시 쓰면 원인 분리가 어려워진다
- metric overlay, env patch, nginx patch처럼 영향 범위를 좁게 나누는 편이 운영상 더 안전했다

### 4. Servermap 먼저, Inspector는 후속 확장으로 접근한다

이유:

- Servermap은 비교적 빠르게 살아난다
- Inspector는 metric stack까지 필요하므로, 문제를 같은 층위로 다루면 원인 파악이 느려진다

결과:

- `UI/경로` 문제와 `metric/Pinot` 문제를 분리해서 해결했다

### 5. Inspector는 유지하되, 1차 튜닝은 heap cap까지만 건다

이유:

- Inspector를 끄면 metric 스택 부담은 줄지만, 실제 운영에서 보고 싶은 메모리/통계 화면이 사라진다
- 반대로 Kafka partition이나 Pinot 재초기화까지 바로 건드리면 가볍게 되돌리기 어렵다

결론:

- 1차는 `heap + mem_limit`만 조정
- 2차는 필요할 때 `pinot-server-0`, Kafka topic partition, Pinot table 구성을 손본다

### 6. SSO도 같은 APM에 묶어서 본다

이유:

- `Comit`만 보면 인증 경계가 black box로 남는다
- 같은 Java 계열 서비스면 agent 주입 비용이 낮고, 실제로 얻는 trace 연결 이점이 크다

결론:

- auth server도 같은 Pinpoint collector로 붙인다
- 단, `applicationName`과 `agentId`는 backend와 분리해 관리한다

## 현재 운영 상태

- `Servermap`: 동작
- `Inspector`: 동작
- backend agent: collector 연결 확인
- SSO agent: collector 연결 확인
- UI 공개 경로:
  - `https://chcse.knu.ac.kr/comit-staging/pinpoint/`
- 메모리 1차 튜닝: 적용
- TLS 인증서:
  - 현재 `*.knu.ac.kr` 인증서 만료 확인, 교체 필요

아직 남아 있는 운영 리스크:

- path 기반 호스팅이라 Pinpoint 프론트 버전 업 시 재패치 가능성이 있다
- 외부 접근 제어가 약하므로 `basic auth` 또는 allowlist를 추가하는 편이 낫다

## 이후 과제

- `pinpoint.chcse.knu.ac.kr` 같은 별도 서브도메인으로 분리
- nginx `basic auth` 또는 내부망/IP allowlist 추가
- `sidowi` 외 다른 Java 서비스로 agent 확장
- SSO 서버까지 같은 trace에 묶을 수 있는지 별도 검토
- 이번 적용 결과를 기준으로 compose/env 템플릿을 실제 상태에 맞게 재정리
