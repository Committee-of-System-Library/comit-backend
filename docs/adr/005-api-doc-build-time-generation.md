# ADR-005. API 문서 생성물(docs/api)을 git에서 제거하고 빌드 시점에 생성

## 상태: 채택

## 날짜: 2026-06-08

> [ADR-001](./001-api-doc-automation.md)의 "CI에서 `git diff docs/api/` 검증 → PR 차단" 결정을 대체한다.
> 관련 이슈: #177

---

## 맥락

ADR-001로 `@ApiContract` 기반 정적 HTML 생성을 도입하면서, 생성물(`docs/api/`)을 **git에 커밋**하고
CI(`validate-api-docs.yml`)가 `git diff docs/api`로 "커밋했는지"를 검증해 PR을 차단해 왔다. 이 방식이 병목이 됐다.

- **수동 단계 강제**: 컨트롤러/`@ApiContract`를 바꾼 사람이 매번 JDK21로 `./gradlew generateApiDocs`를 돌려
  생성물 30여 개 파일을 직접 커밋해야 했다. 빠뜨리면 PR이 차단됐다.
- **멀티팀 머지 충돌**: `docs/api/index.js`·`docs/api/spec-index.js`는 모든 컨트롤러를 한 파일에 모은다.
  Core·ATM·Retention 3팀이 동시에 컨트롤러를 추가하면 이 공유 파일에서 거의 매번 충돌이 났다.
- **그런데 커밋본은 라이브에 꼭 필요하지 않다**: GitHub Pages(`deploy-api-docs-pages.yml`)는 이미 배포 때
  `generateApiDocs`로 **재생성**한 결과를 올린다. 런타임 이미지도 빌드 단계에서 생성한 결과를 `COPY`하면 된다.
  즉 생성물을 git으로 버전 관리할 이유가 약하다.

## 결정

**생성물 `docs/api/`를 git 추적에서 제외하고, 빌드(CI) 시점에 생성한다.**

- `.gitignore`에 `docs/api/` 추가, `git rm -r --cached docs/api`로 추적 해제.
- `deploy.yml`·`deploy-staging.yml`의 `build-and-push`에서 `docker build` 직전에 `generateApiDocs`를 실행
  (`./gradlew clean build generateApiDocs`) → 빌드 컨텍스트에 갓 생성된 `docs/api`가 있으므로
  Dockerfile의 `COPY docs/api /app/api-docs`는 **수정 없이** 그대로 동작한다.
- `validate-api-docs.yml`은 "커밋했는지"(`git diff`) 검증을 제거하고, **`generateApiDocs`가 에러 없이 도는지**만
  본다(깨진 `@ApiContract`/컴파일 실패 조기 발견). 이 스텝이 PR 게이트가 된다.
- `docs/features/`는 **생성물이 아니라 사람이 작성한 입력 소스**이므로 계속 git에 추적한다(제외 대상은 `docs/api`뿐).

## 대안

| 대안 | 거절 이유 |
|---|---|
| 현행 유지(커밋 + git diff 게이트) | 수동 단계·멀티팀 충돌·PR 차단이라는 병목 자체 |
| 생성물 커밋을 봇이 자동 커밋 | 공유 `index.js` 충돌은 그대로, 토큰·재트리거 취약 |
| 생성기(@ApiContract) 폐기·Swagger 이전 | ADR-001에서 이미 검토·거절(어노테이션 인식 불안정·런타임 오버헤드). 별도 결정 사안 |

## 결과

- 컨트롤러/계약만 바꾸면 문서는 빌드가 알아서 따라온다 → **수동 생성·커밋 단계 소멸**.
- 공유 `index.js`/`spec-index.js` **머지 충돌 원천 소멸**.
- 라이브(Pages·런타임 이미지)는 둘 다 빌드 때 재생성분을 쓰므로 **결과물 동일**.
- 트레이드오프: 로컬에서 문서 페이지(`/docs/`, `/api/docs/`)를 보거나 `docker build`를 직접 돌리려면
  사전에 1회 `./gradlew generateApiDocs`가 필요하다(차단 아님, 개발 편의).
- 검증 약화: 게이트가 "생성·컴파일 성공"까지만 보장하고, "특정 변경이 문서에 반영됐는지"는 보지 않는다.
  더 강한 검증이 필요하면 PR에서 생성물을 아티팩트로 업로드/프리뷰하는 방안을 추후 논의(범위 밖).
