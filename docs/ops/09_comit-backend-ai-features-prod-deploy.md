# comit-backend AI 기능 운영 배포 — 장애 원인 분석 및 핫픽스 기록

> 작성일: 2026-06-04  
> 관련 브랜치: `fix/nightsnack-integration-qdrant-ci` (PR #119)  
> 대상 컨테이너: `comit-backend` (운영 staging `/comit-staging/api/`)

---

## 배경

공지사항 RAG 기능(`spring-ai-starter-vector-store-qdrant`, `spring-ai-starter-model-openai`)이 main에 머지된 이후, 운영 배포 파이프라인이 두 가지 이유로 연속 실패했다.

1. **main CI test 게이트 실패** → build-and-push 미실행
2. **self-hosted backend 러너 사망** (~5/18) → deploy 스텝 큐에서 대기만

---

## 장애 1: CI test 실패 — QdrantVectorStore ConnectException

### 원인

`NightSnackConcurrencyIntegrationTest`, `NightSnackReservationIntegrationTest` 두 통합 테스트가 `@SpringBootTest(classes = ComitApplication.class)`로 풀 Spring 컨텍스트를 올린다. 이때 `QdrantVectorStoreAutoConfiguration`이 자동 활성화되어 **컨텍스트 로딩 시점에 Qdrant(gRPC:6334)로 연결을 시도**한다. CI 환경에는 Qdrant가 없으므로 `ConnectException`으로 실패.

### 픽스 (PR #119)

두 통합 테스트 클래스의 `@SpringBootTest.properties`에 추가:

```java
"spring.autoconfigure.exclude="
    + "org.springframework.ai.autoconfigure.vectorstore.qdrant.QdrantVectorStoreAutoConfiguration",
"OPENAI_API_KEY=ci-test-placeholder",   // ChatClient.Builder 빈 생성 허용
"NOTICE_SCHEDULER_ENABLED=false",       // 공지 스케줄러 CI 실행 방지
```

그리고 필드 추가:

```java
@MockitoBean
VectorStore vectorStore;  // NoticeEmbedder 등 VectorStore 주입 빈 대체
```

---

## 장애 2: 운영 컨테이너 재시작 루프 — Flyway V13 out-of-order

### 원인

공지 기능 추가 시 `V13__create_official_notice.sql`이 작성됐는데, 이미 운영 DB에 `V103`, `V106`, `V107`이 적용된 상태였다. V13은 숫자 순서상 이보다 앞이므로 Flyway가 **out-of-order 마이그레이션**으로 간주해 기동을 거부한다.

```
FlywayValidateException: Detected resolved migration not applied to database: 13.
```

### 핫픽스

`/0_services/comit/.comit-prod.env`의 `JAVA_OPTS`에 아래 플래그 추가:

```
-Dspring.flyway.out-of-order=true
```

### 근본 원인 및 권고

마이그레이션 버전 넘버링 규칙을 통일해야 한다. 현재 `V103` 이후가 운영 순서인데 `V13`을 새로 작성하면 이런 문제가 생긴다. 신규 마이그레이션은 **현재 최고 버전(V107) 이후 번호**로 작성할 것. `out-of-order=true`는 임시 조치이며 장기 유지는 권장하지 않는다.

---

## 장애 3: 운영 컨테이너 재시작 루프 — Qdrant/OpenAI 환경변수 미전달

### 원인

`.comit-docker-compose.yml`의 `comit-backend` 서비스 `environment:` 섹션이 AI/RAG 기능 추가 전에 작성되어, **Qdrant·OpenAI 관련 환경변수가 백엔드 컨테이너에 전달되지 않았다**.

- `QDRANT_HOST` 미전달 → 기본값 `localhost:6334` → 연결 거부
- `OPENAI_API_KEY` 미전달 → Spring AI 자동설정이 API 키 없음으로 실패

관련 env var들은 `comit-qdrant` 서비스 environment에는 있지만 `comit-backend`에는 없다.

### 핫픽스

`JAVA_OPTS`(이미 backend environment에 있음)를 통해 Spring 프로퍼티 직접 주입:

```
JAVA_OPTS=-Dspring.flyway.out-of-order=true \
          -Dspring.ai.vectorstore.qdrant.host=192.168.15.120 \
          -Dspring.ai.vectorstore.qdrant.port=6334 \
          -Dspring.ai.vectorstore.qdrant.uses-tls=false \
          -Dspring.ai.openai.api-key=<OPENAI_API_KEY 실제 값>
```

`/0_services/comit/.comit-prod.env`에서 Python(Docker 그룹 우회) + `sed`로 수정.

---

## 현재 상태 (2026-06-04 기준)

| 항목 | 상태 |
|---|---|
| CI main `test` 게이트 | ✅ 통과 (PR #119) |
| `comit-backend:latest` 이미지 | ✅ GHCR 반영 |
| `/comit-staging/api/time` | ✅ 200 정상 |
| `/comit-staging/api/night-snacks?date=...` | ✅ 정상 (데이터 없으면 404) |
| self-hosted backend 러너 | ❌ 사망 (~5/18) — 자동 deploy 불가, 수동 배포 중 |

---

## 정식 수정 필요 사항 (TODO)

### 1. `.comit-docker-compose.yml` `comit-backend` environment 섹션 보완 (필수)

현재 `JAVA_OPTS` 우회 방식은 운영 환경에서 위험하다(배포 시 JAVA_OPTS 덮어쓸 위험, 가독성 저하). 아래 환경변수들을 `comit-backend` service의 `environment:` 섹션에 정식으로 추가해야 한다:

```yaml
- OPENAI_API_KEY=${OPENAI_API_KEY}
- QDRANT_HOST=${QDRANT_HOST}
- QDRANT_PORT=${QDRANT_PORT}
- NOTICE_VECTOR_COLLECTION=${NOTICE_VECTOR_COLLECTION}
- NOTICE_RETRIEVAL_TOP_K=${NOTICE_RETRIEVAL_TOP_K}
- NOTICE_QUERY_TRANSFORM_MODEL=${NOTICE_QUERY_TRANSFORM_MODEL}
- NOTICE_RERANK_MODEL=${NOTICE_RERANK_MODEL}
- NOTICE_ANSWER_NANO_MODEL=${NOTICE_ANSWER_NANO_MODEL}
- NOTICE_ANSWER_MINI_MODEL=${NOTICE_ANSWER_MINI_MODEL}
- NOTICE_ANSWER_MODEL=${NOTICE_ANSWER_MODEL}
- NOTICE_SUMMARIZER_MODEL=${NOTICE_SUMMARIZER_MODEL}
- NOTICE_SCHEDULER_ENABLED=${NOTICE_SCHEDULER_ENABLED}
- NOTICE_INITIAL_SYNC_MAX=${NOTICE_INITIAL_SYNC_MAX}
- NOTICE_LATEST_SYNC_MAX_PAGES=${NOTICE_LATEST_SYNC_MAX_PAGES}
- NOTICE_REINDEX_EMBEDDINGS_ON_STARTUP=${NOTICE_REINDEX_EMBEDDINGS_ON_STARTUP}
- NOTICE_REINDEX_EMBEDDINGS_LIMIT=${NOTICE_REINDEX_EMBEDDINGS_LIMIT}
- NOTICE_CHAT_OPENAI_TIMEOUT=${NOTICE_CHAT_OPENAI_TIMEOUT}
- NOTICE_CHAT_MAX_CONCURRENCY=${NOTICE_CHAT_MAX_CONCURRENCY}
- NOTICE_CHAT_ACQUIRE_TIMEOUT_SECONDS=${NOTICE_CHAT_ACQUIRE_TIMEOUT_SECONDS}
- NOTICE_CHAT_RESPONSE_TIMEOUT_SECONDS=${NOTICE_CHAT_RESPONSE_TIMEOUT_SECONDS}
```

완료 후 `JAVA_OPTS`를 원래 값(비어 있음)으로 복구.

### 2. Flyway 마이그레이션 버전 넘버링 정책 수립

- 신규 마이그레이션은 `V108` 이후 번호 사용 (현재 최고: V107)
- `out-of-order=true` JAVA_OPTS 플래그는 compose 파일 정식 수정 완료 후 제거 가능 (단, V13이 운영 DB에 적용됐으므로 이 플래그 자체는 무해)

### 3. self-hosted backend 러너 복구

`/0_services/comit/backend/` 경로의 GitHub Actions runner 프로세스 재시작. 복구 완료 시 main 머지 → 자동 배포 파이프라인 재개.
