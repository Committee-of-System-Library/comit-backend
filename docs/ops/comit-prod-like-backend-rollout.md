# Comit Prod-Like Backend Rollout Plan

SSO가 아직 완전히 연결되지 않은 상태에서, `Comit` 백엔드를 운영과 최대한 비슷한 환경으로 먼저 띄우기 위한 구현 계획.

이 문서는 "정식 운영 오픈"이 아니라 "닫힌 prod-like 환경"을 목표로 한다.

## 왜 prod-like가 먼저 필요한가

- 현재 인증 주입은 [local 전용 헤더 브리지](../../src/main/java/kr/ac/knu/comit/global/auth/MemberAuthenticationFilter.java)에만 있다.
- 실제 API는 대부분 `@AuthenticatedMember`를 요구하므로, prod에서 인증 공급자가 없으면 주요 기능이 전부 `401`이 된다.
- 반대로 `local` 프로필을 서버에 그대로 올리면 포트, DB, multipart, JPA 기본값이 운영 환경과 맞지 않는다.
- 따라서 `prod 설정 + 닫힌 임시 인증 브리지`를 가진 `prod-like` 단계를 먼저 만들어야 한다.

## 목표 상태

- 외부 공개 경로는 정식 `/comit`가 아니라 닫힌 staging 경로를 사용한다.
- 백엔드는 운영과 같은 포트, Docker, MySQL, Flyway, nginx 프록시 구조로 구동한다.
- 인증만 임시 브리지로 우회 가능하게 둔다.
- `sidowi`에서 GHCR 이미지 pull + `docker compose up -d`로 재배포 가능해야 한다.
- 기능 검증 대상 API는 실제 FE 연동이 가능한 수준까지 열린다.

## 제안하는 운영 형태

### 경로

- FE: `/comit-staging`
- BE: `/comit-staging/api`

### Spring 프로필

- `staging`

### 노출 정책

- 외부 공개 금지
- 아래 둘 중 하나를 반드시 적용
  - nginx `Basic Auth`
  - 학교 내부망 또는 허용 IP만 접근 가능

### DB 전략

- 운영 DB와 같은 MySQL 인스턴스를 써도 되지만 스키마는 반드시 분리
- 권장 스키마 이름: `comit_staging`

## 비목표

- 정식 SSO/Keycloak 연동 완료
- `/comit` 정식 운영 오픈
- 프론트 공개 배포
- 운영 데이터 마이그레이션

## 현재 확인된 갭

### 인증

- [MemberAuthenticationFilter.java](../../src/main/java/kr/ac/knu/comit/global/auth/MemberAuthenticationFilter.java)는 현재 `comit.auth.bridge.enabled=true` 일 때만 동작한다.
- [MemberArgumentResolver.java](../../src/main/java/kr/ac/knu/comit/global/auth/MemberArgumentResolver.java)는 request attribute에 `MemberPrincipal`이 없으면 `401`을 반환한다.
- 결과적으로 real SSO 없이 prod-like를 쓰려면 staging에서 임시 브리지 활성화와 보호 정책이 함께 필요하다.

### 런타임 설정

- [application.yml](../../src/main/resources/application.yml)는 `SPRING_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAX_FILE_SIZE`, `MAX_REQUEST_SIZE`에 기본값이 없다.
- 같은 파일의 `DDL_AUTO` 기본값은 `create-drop`이다.
- 운영 env가 비어 있으면 첫 부팅부터 실패하거나, 잘못된 기본값을 타면 위험하다.

### 배포 자동화

- [deploy.yml](../../.github/workflows/deploy.yml)은 현재 GHCR push 후 self-hosted runner에서 `knu-cse-comit-server`를 재기동하도록 정리돼 있다.
- 남은 갭은 workflow가 아니라 `sidowi` 쪽 compose 서비스, env 파일, nginx 라우팅이 아직 없다는 점이다.

### 운영 인프라

- `sidowi`의 실제 운영 기준은 `/opt/docker/compose/docker-compose.services.yml`과 `/opt/docker/nginx/conf.d/*.conf`다.
- 여기에 `knu-cse-comit-server`, `knu-cse-comit-client`, `/comit-staging`, `/comit-staging/api`가 아직 없다.

## 작업 스트림

### Stream 1. 프로필과 설정 정리

### S1-1. `staging` 프로필 신설

- 목적: `local`과 `prod`를 섞지 않고, 운영과 유사한 닫힌 환경을 분리한다.
- 대상 파일
  - `src/main/resources/application-staging.yml` 신규
  - `src/main/resources/application.yml`
- 해야 할 일
  - `SPRING_PORT=8080` 기준으로 정리
  - `DDL_AUTO=none` 명시
  - Flyway 사용 전제 확인
  - Spring Boot 4 런타임 classpath에 `spring-boot-starter-flyway`가 포함되어 자동 migration이 실제로 실행되게 함
  - multipart 기본값 명시
  - 로그 파일 경로를 컨테이너 로그 볼륨과 맞춤
- 산출물
  - staging 전용 설정 파일
  - 운영 필수 env 목록

### S1-2. 운영 필수 env 계약 문서화

- 목적: `sidowi`의 `/opt/docker/env/comit.env`를 실수 없이 만들 수 있게 한다.
- 대상 파일
  - 이 문서
  - 필요 시 `docs/ops/env.md` 신규
- 해야 할 일
  - 필수 env와 선택 env를 분리
  - 기본값 허용 여부를 명시
  - staging에서만 필요한 브리지 플래그를 추가
- 최소 필수 env 초안
  - `SPRING_PROFILES_ACTIVE=staging`
  - `SPRING_PORT=8080`
  - `DB_URL=jdbc:mysql://cse-db:3306/comit_staging?...`
  - `DB_USERNAME=<staging user>`
  - `DB_PASSWORD=<staging password>`
  - `DDL_AUTO=none`
  - `MAX_FILE_SIZE=10MB`
  - `MAX_REQUEST_SIZE=10MB`
  - `LOG_FILE_PATH=/app/logs/app-staging.log`
  - `VIRTUAL_THREADS_ENABLED=true`
  - `COMIT_AUTH_BRIDGE_ENABLED=true`

### S1-3. 위험한 기본값 제거

- 목적: env 누락 시 위험한 동작을 막는다.
- 대상 파일
  - `src/main/resources/application.yml`
- 해야 할 일
  - `DDL_AUTO:create-drop` 같은 위험한 fallback 제거 또는 staging/prod에서 덮어쓰기 강제
  - 운영에서 절대 비어 있으면 안 되는 env는 문서와 코드에서 동시에 명확히 함
- 완료 조건
  - staging/prod 부팅 경로에서 destructive 기본값이 남아 있지 않음

### Stream 2. 임시 인증 브리지 설계

### S2-1. `local` 전용 브리지를 프로퍼티 기반으로 분리

- 목적: `local`과 `staging`에서 같은 임시 인증 코드를 재사용하되, prod에는 절대 열리지 않게 한다.
- 대상 파일
  - [MemberAuthenticationFilter.java](../../src/main/java/kr/ac/knu/comit/global/auth/MemberAuthenticationFilter.java)
  - auth 설정 관련 신규 파일
- 해야 할 일
  - `@Profile("local")` 대신 `COMIT_AUTH_BRIDGE_ENABLED` 같은 플래그 기반으로 변경
  - `staging`에서만 활성화되도록 문서화
  - prod에서는 false가 기본이 되게 함
- 주의
  - 정식 SSO 연동이 들어오면 이 브리지는 바로 제거 대상임을 문서에 명시

### S2-2. 브리지 접근 자체를 닫힌 환경으로 제한

- 목적: 헤더 기반 가짜 인증이 외부에 그대로 노출되는 사고를 막는다.
- 대상 파일
  - `/opt/docker/nginx/conf.d/comit.conf`
  - 필요 시 nginx 공통 auth 파일
- 해야 할 일
  - `/comit-staging` 전체에 Basic Auth 또는 IP allowlist 적용
  - 브리지 헤더를 외부 누구나 넣을 수 없게 보호
- 완료 조건
  - 인증 브리지가 켜져 있어도 공개 서비스처럼 접근할 수 없음

### S2-3. `MemberPrincipal` 주입 경로를 staging에서 검증

- 목적: 실제 API가 prod-like에서 `401` 없이 동작하는지 확인
- 대상 파일
  - [MemberArgumentResolver.java](../../src/main/java/kr/ac/knu/comit/global/auth/MemberArgumentResolver.java)
  - 관련 테스트
- 해야 할 일
  - 브리지 필터가 넣은 principal이 모든 인증 API에서 통일되게 읽히는지 확인
  - 잘못된 헤더 또는 누락 시의 동작을 명확히 함

### Stream 3. 백엔드 배포 자동화

### S3-1. GHCR 이미지명 표준화

- 목적: 기존 서비스와 같은 운영 네이밍을 사용
- 대상 파일
  - [deploy.yml](../../.github/workflows/deploy.yml)
- 해야 할 일
  - `IMAGE_NAME: knu-cse-comit-server`로 변경
  - 필요 시 timestamp tag 정책 유지
- 완료 조건
  - 운영 compose 서비스명과 GHCR 이미지명이 한눈에 연결됨

### S3-2. self-hosted deploy job 추가

- 목적: `main` 머지 후 실제 `sidowi` 컨테이너가 갱신되게 함
- 대상 파일
  - [deploy.yml](../../.github/workflows/deploy.yml)
- 해야 할 일
  - `build-and-push` 이후 `deploy` job 추가
  - `runs-on: [self-hosted, backend]` 또는 `comit_BE`용 label 전략 확정
  - `docker pull ghcr.io/committee-of-system-library/knu-cse-comit-server:latest`
  - `docker compose -f /opt/docker/compose/docker-compose.services.yml up -d knu-cse-comit-server`
  - `docker image prune -f`
- 의존성
  - `sidowi`에 backend runner 준비 완료
  - compose에 서비스 등록 완료

### S3-3. 수동 재배포 절차 문서화

- 목적: Actions 장애 시에도 복구 가능하게 함
- 대상 파일
  - 이 문서
  - 필요 시 `docs/ops/deploy.md` 신규
- 해야 할 일
  - 수동 명령어를 남김
  - rollback 명령까지 같이 기록

### Stream 4. `sidowi` 인프라 온보딩

### S4-1. compose 서비스 추가

- 목적: 운영 서버가 `Comit` 컨테이너를 서비스로 인식하게 함
- 대상 파일
  - `/opt/docker/compose/docker-compose.services.yml`
- 해야 할 일
  - `knu-cse-comit-server`
  - 추후 FE용 `knu-cse-comit-client`
  - 네트워크는 `cse-proxy`, `cse-backend`
  - `restart: unless-stopped`
- 참고
  - 기존 live 서비스는 `knu-cse-auth-server`, `knu-cse-locker-server`, `knu-cse-ledger-server` 패턴을 따른다.

### S4-2. staging nginx 라우팅 추가

- 목적: 외부에서 닫힌 staging URL로 접근 가능하게 함
- 대상 파일
  - `/opt/docker/nginx/conf.d/comit.conf` 신규
  - `/opt/docker/nginx/conf.d/chcse.knu.ac.kr.conf`
- 해야 할 일
  - `/comit-staging` -> FE
  - `/comit-staging/api/` -> BE
  - `X-Forwarded-*` 헤더 정리
  - Basic Auth 또는 allowlist 적용
- 결정 필요
  - 백엔드가 `/comit-staging/api` prefix를 nginx rewrite로 처리할지, Spring context-path로 맞출지
- 권장
  - v1은 nginx rewrite로 처리하고 앱은 `/posts`, `/members/me` 등 현재 경로를 유지

### S4-3. staging DB 및 계정 준비

- 목적: 운영 DB와 분리된 안전한 검증 환경 확보
- 대상 위치
  - `cse-db` MySQL 내부
  - `/opt/docker/env/comit.env`
- 해야 할 일
  - `comit_staging` 스키마 생성
  - staging 전용 DB 계정 생성
  - Flyway 권한 부여 여부 확정
  - `/opt/docker/env/comit.env` 작성
- 완료 조건
  - backend 컨테이너가 staging DB에 연결되고 migration이 끝남

### S4-4. runner 준비

- 목적: `comit`만 재배포하는 self-hosted backend runner를 붙임
- 대상 위치
  - `/0_services/comit/backend`
  - systemd service
- 해야 할 일
  - runner 설치
  - `backend` label 또는 `comit_BE` 전용 label 결정
  - systemd 등록
  - `docker` 그룹 권한 확인
- 참고
  - 기존 서버는 `/0_services/sso/backend`, `/0_services/locker/backend`, `/0_services/ledger/backend` 구조를 사용한다.

### Stream 5. 검증과 오픈 조건

### S5-1. 컨테이너 레벨 검증

- 목적: 첫 부팅 실패를 가장 먼저 잡음
- 해야 할 일
  - `docker ps` 확인
  - `docker logs knu-cse-comit-server --tail 100`
  - DB 연결, Flyway, 포트 바인딩 확인

### S5-2. HTTP 레벨 검증

- 목적: nginx -> backend 라우팅 확인
- 해야 할 일
  - `curl -I https://chcse.knu.ac.kr/comit-staging/api/posts?...`
  - 인증 없는 상태와 인증 헤더 있는 상태를 모두 확인
  - rewrite/context-path 중복 여부 확인

### S5-3. 기능 스모크 테스트

- 목적: staging에서 실제 사용 흐름 검증
- 검증 대상
  - 내 프로필 조회
  - 게시글 목록/상세/인기글
  - 게시글 작성/좋아요/조회수
  - 댓글/대댓글/도움이요
- 완료 조건
  - FE 연동 전에도 Postman 또는 curl 기준으로 핵심 API가 모두 정상 동작

### S5-4. 제거 조건 명시

- 목적: 임시 브리지 상태가 장기화되지 않게 함
- 해야 할 일
  - 정식 SSO 붙는 즉시 제거할 항목을 문서화
  - 제거 대상
    - `COMIT_AUTH_BRIDGE_ENABLED`
    - 헤더 기반 임시 인증
    - staging 전용 Basic Auth

## 추천 구현 순서

1. `staging` 프로필과 env 계약 정리
2. 임시 인증 브리지 플래그화
3. staging DB 준비
4. backend deploy workflow 수정
5. `sidowi` compose에 `knu-cse-comit-server` 추가
6. `/comit-staging/api` nginx 라우팅 추가
7. self-hosted backend runner 연결
8. GHCR push -> sidowi 자동 재배포 검증
9. API smoke test
10. 프론트 온보딩은 그 다음 단계로 진행

## 백엔드 작업 체크리스트

- [ ] `staging` 프로필 파일 추가
- [ ] 운영 필수 env 목록 정리
- [ ] 위험한 기본값 제거 또는 staging/prod에서 명시적 덮어쓰기
- [ ] 인증 브리지 플래그 기반 활성화로 전환
- [ ] staging 보호 정책 결정
- [ ] backend deploy workflow를 `pull + compose up -d`까지 확장
- [ ] GHCR 이미지명 `knu-cse-comit-server`로 정리
- [ ] `/opt/docker/compose/docker-compose.services.yml`에 backend 등록
- [ ] `/opt/docker/nginx/conf.d/comit.conf` 작성
- [ ] staging DB와 계정 준비
- [ ] `/opt/docker/env/comit.env` 작성
- [ ] backend runner 등록
- [ ] 첫 자동 배포 성공 확인
- [ ] staging smoke test 통과

## 완료의 정의

- `main`에 머지하면 `knu-cse-comit-server:latest`가 GHCR에 올라간다.
- 같은 변경이 `sidowi`에서 자동으로 pull되어 `knu-cse-comit-server` 컨테이너가 재기동된다.
- `https://chcse.knu.ac.kr/comit-staging/api` 아래 주요 API가 staging 보호 하에서 접근 가능하다.
- 인증 브리지로 API 기능 검증이 가능하고, prod 정식 오픈 전까지 외부 공개는 차단된다.
