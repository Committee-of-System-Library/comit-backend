# 메인 페이지 (Main Page)

## 1. Overview

### 1.1 Goal
- 비로그인 사용자를 포함한 모든 방문자가 메인 페이지에서 서비스 핵심 콘텐츠를 한눈에 볼 수 있어야 한다.
- 단일 API 호출로 QNA·INFO·FREE 각 5개, NOTICE·EVENT 각 3개, 인기글 5개를 반환해 FE 다중 호출을 제거한다.

### 1.2 In Scope
- `GET /main` 퍼블릭 엔드포인트 (인증 불필요)
- QNA·INFO·FREE 최신 5개 게시글 요약
- NOTICE·EVENT 최신 3개 게시글 요약
- Hot Post 상위 5개 (기존 집계 로직 재사용)

### 1.3 Out of Scope
- 비로그인 상태의 좋아요 여부 표시
- 실시간 갱신 / 캐시 전략 (후속 작업)
- 섹션별 정책 개수 초과 조회

### 1.4 Success Signal
- 비로그인 상태에서 `GET /main` 호출 시 200 응답과 모든 섹션 데이터가 반환된다.
- QNA·INFO·FREE는 최대 5개, NOTICE·EVENT는 최대 3개이며, 데이터가 없으면 빈 배열을 반환한다.
- 기존 인증 필요 API의 동작에 영향이 없다.

---

## 2. Domain Context

### 2.1 Domain Terms
- **섹션**: 게시판 타입별 또는 인기글 기준으로 묶인 게시글 그룹
- **메인 페이지**: 인증 없이 접근 가능한 서비스 진입점

### 2.2 Actors
- **비로그인 방문자**: 인증 토큰 없이 접근
- **로그인 사용자**: 인증 토큰과 함께 접근 (동일 응답)

### 2.3 Assumptions
- 각 섹션의 정렬 기준은 최신순(id DESC)이다.
- Hot Post 집계 로직은 기존 `PostService.getHotPosts()`를 그대로 재사용한다.
- `hiddenByAdmin = true` 또는 `deletedAt IS NOT NULL`인 게시글은 노출하지 않는다.

---

## 3. Scenarios

### Scenario A. 비로그인 사용자가 메인 페이지를 조회한다
Given:
- 인증 토큰 없음
- 각 게시판에 게시글이 존재함

When:
- `GET /main` 호출

Then:
- 200 OK
- qna, info, free는 각 최대 5개, notice, event는 각 최대 3개, hotPosts는 최대 5개 반환

### Scenario B. 특정 게시판에 게시글이 없다
Given:
- EVENT 게시판에 게시글 0개

When:
- `GET /main` 호출

Then:
- `event: []` 빈 배열 반환, 나머지 섹션은 정상

### Scenario C. 인기글 점수가 0인 게시글만 있다
Given:
- 최근 7일 반응이 없어 모든 게시글 점수 0

When:
- `GET /main` 호출

Then:
- `hotPosts: []` 빈 배열 반환 (기존 Hot Post 정책 동일)

---

## 4. Functional Requirements
- FR-1: `GET /main`은 인증 없이 호출 가능해야 한다.
- FR-2: QNA·INFO·FREE는 각 게시판 최신 5개를 반환하고, NOTICE·EVENT는 각 게시판 최신 3개를 반환한다.
- FR-3: Hot Post 상위 5개를 함께 반환한다.
- FR-4: 각 섹션이 비어 있으면 빈 배열을 반환한다 (null 금지).
- FR-5: soft delete 또는 관리자 숨김 처리된 게시글은 제외한다.

---

## 5. Behavioral Rules

### 5.1 Preconditions
- 없음 (퍼블릭 엔드포인트)

### 5.2 Postconditions
- 읽기 전용. 어떤 상태도 변경되지 않는다.

### 5.3 Invariants
- 각 섹션은 항상 List 타입이며 null이 아니다.
- 삭제되거나 숨김 처리된 게시글은 절대 포함되지 않는다.

### 5.4 Forbidden Rules
- 인증 실패(401)를 반환해서는 안 된다.
- 게시글 본문 전체를 반환해서는 안 된다 (미리보기만).

---

## 7. External Contracts

### 7.1 API Contract
- **Endpoint**: `GET /main`
- **Auth**: 불필요
- **Request**: 없음
- **Response**:
```json
{
  "result": "SUCCESS",
  "data": {
    "qna":     [/* PostSummaryResponse 최대 5개 */],
    "info":    [/* PostSummaryResponse 최대 5개 */],
    "free":    [/* PostSummaryResponse 최대 5개 */],
    "notice":  [/* PostSummaryResponse 최대 3개 */],
    "event":   [/* PostSummaryResponse 최대 3개 */],
    "hotPosts":[/* HotPostResponse 최대 5개 */]
  }
}
```
- **Error cases**: 없음 (서버 오류 시 500)

### 7.2 Persistence Contract
- DB 변경 없음. 읽기 전용.

---

## 8. Non-Functional Constraints

### 8.1 Technical Constraints
- 게시판별 최신 목록 조회는 메인 페이지 전용 읽기 계층으로 캡슐화한다.
- `PostService.getHotPosts()` 재사용
- 새 클래스: `MainController`, `MainService`, `MainPageQueryService`, `MainPageResponse`
- 패키지: `kr.ac.knu.comit.main`

### 8.2 Security Constraints
- Security 설정에서 `GET /main` permitAll 추가 필요
- 인증 컨텍스트에 접근하지 않는다 (`MemberPrincipal` 파라미터 없음)

### 8.3 Performance Constraints
- DB 쿼리 목표: BoardType 5개 × 1쿼리 + HotPost 1쿼리 수준을 유지한다.
- 이 목표를 위해 메인 전용 읽기 계층에서 목록 조립 방식을 최적화할 수 있어야 한다.
- 향후 트래픽이 늘면 전체 응답 캐시 적용 고려 (현재 범위 외)

---

## 9. Test Criteria

### 9.1 Happy Path
- 모든 게시판에 데이터가 있을 때 qna/info/free는 5개 이하, notice/event는 3개 이하로 반환
- 인증 토큰 없이 200 응답

### 9.2 Failure Cases
- 없음 (인증 없음, 쓰기 없음)

### 9.3 Edge Cases
- 특정 게시판 게시글 0개 → 해당 섹션 빈 배열
- 인기글 점수 0 → hotPosts 빈 배열
- 게시글이 섹션 최대 개수보다 적은 게시판 → 있는 만큼만 반환

### 9.4 Regression Risks
- 기존 `GET /posts?boardType=QNA` 등 인증 필요 API 동작 변경 없어야 함
- Hot Post 집계 로직 변경 없어야 함

---

## 10. Open Questions
- 메인 페이지 응답에 캐시(TTL)를 걸 것인가? (Phase 2 검토)
- `PostSummaryResponse` 재사용 vs 메인 전용 DTO 분리?
- `MainPageQueryService`가 Hot Post까지 담당할지, Hot Post는 기존 `PostService` 재사용을 유지할지?
- `enableSsoStarter=true` 환경에서 `GET /main` permitAll 설정을 이 저장소 안에서 할지, SSO starter 통합 설정 레이어에서 할지?

---

## 11. Implementation Guardrails
- `MainService`는 메인 페이지 유스케이스 조합만 담당하고, 게시판별 목록 조회 세부사항은 `MainPageQueryService`로 숨긴다.
- `MainService`는 JPA 리포지토리를 직접 의존하지 않는다.
- DTO는 기존 `PostSummaryResponse`, `HotPostResponse` 재사용을 우선 검토한다.
- 테스트: `GET /main` 비인증 호출 → 200, 각 섹션 List 타입 검증 포함

---

## 12. Implementation Plan

### 12.1 구현 방향
- 새 패키지는 명세대로 `kr.ac.knu.comit.main`을 사용한다.
- API surface는 `GET /main` 하나로 고정하고, 응답은 `ApiResponse<MainPageResponse>` 형태로 반환한다.
- `MainController`는 인증 컨텍스트를 받지 않고 `MainService`를 한 번만 호출하는 thin controller로 구현한다.
- `MainService`는 read-only 유스케이스 오케스트레이터로 두고, 게시판별 최신 목록은 `MainPageQueryService`를 통해 조회한다.
- `MainPageQueryService`는 메인 페이지 전용 읽기 모델을 책임지고, 게시판별 최신 목록 조회에 필요한 정렬/가시성/응답 조립 세부사항을 캡슐화한다.
- Hot Post는 우선 기존 `PostService.getHotPosts()`를 재사용한다.
- 메인 페이지 섹션 DTO는 새로 만들지 않고 기존 `PostSummaryResponse`, `HotPostResponse`를 재사용한다.

### 12.2 생성 대상 파일
- `src/main/java/kr/ac/knu/comit/main/controller/api/MainControllerApi.java`
- `src/main/java/kr/ac/knu/comit/main/controller/MainController.java`
- `src/main/java/kr/ac/knu/comit/main/service/MainService.java`
- `src/main/java/kr/ac/knu/comit/main/service/MainPageQueryService.java`
- `src/main/java/kr/ac/knu/comit/main/dto/MainPageResponse.java`
- `src/test/java/kr/ac/knu/comit/main/service/MainServiceTest.java`
- `src/test/java/kr/ac/knu/comit/main/service/MainPageQueryServiceTest.java` 또는 리포지토리/통합 테스트
- `src/test/java/kr/ac/knu/comit/api/AuthenticatedApiWebTest.java` 또는 `MainControllerWebTest`

### 12.3 API 계약 구현 방식
- `MainControllerApi`에 `@ApiContract`, `@RequestMapping("/main")`, `@GetMapping`을 선언한다.
- `@ApiDoc`에는 `qna`, `info`, `free`, `notice`, `event`, `hotPosts` 각 필드 설명과 예시를 넣는다.
- 성공 응답은 모든 섹션이 항상 List로 내려가도록 보장한다. `null`은 허용하지 않는다.
- 인증 실패 에러는 문서화하지 않는다. 이 엔드포인트는 퍼블릭이기 때문이다.

### 12.4 서비스 흐름
1. `MainService.getMainPage()`를 공개 메서드로 둔다.
2. 내부에서 `mainPageQueryService.getSections(5)`를 호출해 `qna`, `info`, `free`, `notice`, `event` 섹션 데이터를 한 번에 받는다. 이때 `notice`, `event`는 내부 정책으로 3개까지만 조회한다.
3. `postService.getHotPosts().posts()`로 `hotPosts`를 채운다.
4. 위 결과를 `MainPageResponse`에 모아 반환한다.

권장 helper 구조:

```java
public MainPageResponse getMainPage() {
    MainPageSections sections = mainPageQueryService.getSections(SECTION_SIZE);
    return new MainPageResponse(
        sections.qna(),
        sections.info(),
        sections.free(),
        sections.notice(),
        sections.event(),
        postService.getHotPosts().posts()
    );
}
```

```java
public MainPageSections getSections(int size) {
    return new MainPageSections(
        getSection(BoardType.QNA, size),
        getSection(BoardType.INFO, size),
        getSection(BoardType.FREE, size),
        getSection(BoardType.NOTICE, size),
        getSection(BoardType.EVENT, size)
    );
}
```

### 12.5 테스트 계획

#### 서비스 테스트
- `MainServiceTest`
- `mainPageQueryService.getSections(5)`와 `postService.getHotPosts()` 결과를 조합해 반환한다.
  - 특정 게시판 결과가 비어 있어도 빈 리스트를 유지한다.
  - `hotPosts`가 비어 있으면 빈 리스트를 유지한다.

#### 조회 계층 테스트
- `MainPageQueryServiceTest` 또는 리포지토리/통합 테스트
- QNA·INFO·FREE는 최대 5개, NOTICE·EVENT는 최대 3개만 조회되는지 검증한다.
  - 삭제/숨김 게시글이 제외되는지 검증한다.
  - 데이터가 없는 게시판은 빈 리스트가 되는지 검증한다.
  - `PostSummaryResponse` 조립 시 본문 전체가 아닌 preview만 포함되는지 검증한다.

#### 웹 테스트
- 비인증 요청으로 `GET /main` 호출 시 `200 OK`
- `$.result == "SUCCESS"`
- `$.data.qna`, `$.data.info`, `$.data.free`, `$.data.notice`, `$.data.event`, `$.data.hotPosts`가 모두 배열
- 특정 섹션이 비어 있어도 `[]`로 직렬화되는지 검증

#### 문서/생성기 테스트
- `generateApiDocs` 실행 후 `docs/api/main/MainControllerApi.html` 생성 여부 확인
- `docs/api/index.js`에 `MainControllerApi`가 추가되는지 확인

### 12.6 문서화 및 산출물 반영 순서
1. `MainControllerApi` 작성
2. `MainPageQueryService`, `MainPageResponse` 구현
3. `MainController`, `MainService` 구현
4. 서비스 테스트, 조회 계층 테스트, 웹 테스트 추가
5. `./gradlew test generateApiDocs` 실행
6. `docs/api/main/MainControllerApi.html`, `docs/api/index.js` 생성물 확인

### 12.7 구현 시 체크포인트
- `MainService`가 `BoardType`별 분기와 응답 조립 세부사항까지 들고 있지 않도록 한다. 섹션별 목록 조회는 `MainPageQueryService` 안으로 넣는다.
- `MainPageQueryService`는 메인 페이지 전용 read model 계층으로 취급하고, 이후 섹션 추가/정렬 기준 변경/캐시 도입 시 수정 지점을 여기로 모은다.
- 현재 저장소 안에는 명시적인 `SecurityFilterChain`이 보이지 않는다. 따라서 `GET /main` 퍼블릭 보장은 이 저장소 코드만으로 끝나는지, SSO starter 활성 환경에서 별도 allowlist가 필요한지 구현 전에 확인한다.
- Hot Post는 기존 `PostService.getHotPosts()` 재사용을 우선하되, 메인 페이지 최적화가 필요해지면 `MainPageQueryService` 쪽으로 read model을 확장할 수 있게 열어둔다.
