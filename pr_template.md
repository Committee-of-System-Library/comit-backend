## PR 작성 가이드

> `.github/pull_request_template.md`가 GitHub PR 생성 시 자동 삽입됩니다.
> 이 파일은 각 섹션을 어떻게 채울지 설명하는 참고 문서입니다.

---

### 관련 이슈
연결된 이슈 번호. `resolves #123` 으로 쓰면 PR 머지 시 이슈가 자동 닫힙니다.

---

### 변경 요약
**2-3줄로 끝냅니다.** 무엇을 바꿨는지가 아니라 왜 바꿨는지에 집중하세요.

```
- QdrantVectorStore 초기화 시 OpenAI API 키가 없으면 앱이 뜨지 않는 문제 수정
- local 프로파일에서 실제 OpenAI 호출 없이 기동 가능하도록 mock 빈 추가
```

---

### 의사결정
대안이 있었다면 왜 이걸 선택했는지 한 줄 근거와 함께 씁니다.
근거 없는 선택은 리뷰어가 "왜 이렇게?" 를 댓글로 물어봐야 해서 라운드트립이 생깁니다.

```
- EmbeddingModel 인터페이스 대신 AbstractEmbeddingModel을 상속
  → dimensions()가 인터페이스가 아닌 AbstractEmbeddingModel에 정의되어 있어
    인터페이스 구현만으로는 QdrantVectorStore가 mock을 주입받지 못함
```

---

### 리뷰 포인트
**리뷰어가 이해하기 어려울 수 있는 라인·설계를 콕 찝어 설명합니다.**
"이 부분은 X 때문에 Y처럼 썼습니다"가 핵심 포맷.

```
- LocalAiConfig:21 — @Primary가 없으면 OpenAiEmbeddingModel이 우선 주입됨.
  local 프로파일에서만 이 mock이 이겨야 하므로 @Primary 필수.
- MemberAuthenticationFilter:128 — requestUri.endsWith()를 쓴 이유:
  SsoAuthenticationFilter는 servletPath(context path 제외)를 쓰지만
  이 필터는 requestURI를 써서 endsWith로 suffix 매칭이 더 안전.
```

---

### 테스트
리뷰어가 직접 확인할 수 있는 체크리스트. 자동화된 테스트는 생략해도 됩니다.

```
- [ ] local 프로파일로 앱 기동 시 오류 없이 시작되는지
- [ ] /members/nicknames/check 미인증 상태에서 200 반환 확인
```
