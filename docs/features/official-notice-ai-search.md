# 컴퓨터학부 공지사항 AI 검색

## 1. Overview

### 1.1 Goal
- 경북대학교 컴퓨터학부 공식 공지사항을 Comit 안에서 검색하고 요약할 수 있게 한다.
- 기존 학부 홈페이지 공지사항 접근성이 낮아 필요한 정보를 놓치는 문제를 줄인다.
- 크롤링된 공지 원문을 저장하고, 벡터 임베딩 기반 검색과 AI 요약을 제공한다.

### 1.2 In Scope
- 컴퓨터학부 공지사항 페이지 최신 글 크롤링 및 저장
- Spring AI VectorStore에 공지 검색 문서 저장
- 검색어 기반 의미 검색 API
- 공지 상세 조회 API
- 공지 AI 요약 API

### 1.3 Out of Scope
- 주기적 배치 스케줄러
- 첨부파일 다운로드 및 전문 추출
- 관리자 UI
- DB 네이티브 vector index 도입

### 1.4 Success Signal
- 스케줄러가 주기적으로 최신 공지를 확인하고 새 공지가 있으면 저장 및 VectorStore 반영을 수행한다.
- 사용자가 자연어 검색어로 관련 공지 목록을 조회할 수 있다.
- 사용자가 특정 공지를 AI 요약으로 확인할 수 있다.

## 2. Domain Context

### 2.1 Domain Terms
- 공식 공지사항: 경북대학교 컴퓨터학부 홈페이지의 `공지사항` 게시판에 올라온 글
- sourceId: 외부 게시판의 `wr_id`
- VectorStore 검색 문서: 공지 제목과 본문을 벡터 DB에 저장하기 위한 Spring AI `Document`
- 요약: 공지 원문을 학생이 빠르게 이해할 수 있도록 압축한 AI 생성 텍스트

### 2.2 Actors
- 사용자: 저장된 공지를 검색하고 상세/요약을 조회한다.
- 스케줄러: 최신 공지사항 동기화를 실행한다.
- 외부 시스템: 경북대학교 컴퓨터학부 홈페이지, AI 임베딩/요약 제공자

### 2.3 Assumptions
- 외부 공지사항 상세 URL에는 안정적인 `wr_id`가 포함된다.
- 운영 환경은 `OPENAI_API_KEY`를 설정해 Spring AI OpenAI 모델을 사용할 수 있다.
- API 키가 없는 환경에서는 공지사항 AI 처리가 `NOTICE_AI_FAILED`로 실패한다.

## 3. Scenarios

### Scenario A. 최신 공지 스케줄 동기화
Given:
- 컴퓨터학부 공지사항 페이지가 접근 가능하다.
- 공지사항 동기화 스케줄러가 활성화되어 있다.

When:
- 스케줄 주기가 도래한다.

Then:
- 최신 공지 목록에서 sourceId와 상세 URL을 먼저 수집한다.
- 이미 저장된 sourceId는 상세 페이지 크롤링 대상에서 제외한다.
- 아직 저장되지 않은 새 공지는 짧은 DB 트랜잭션 안에서 생성된다.
- DB 트랜잭션 완료 후 새 공지 제목/본문이 Spring AI VectorStore에 저장된다.

### Scenario B. 공지 검색
Given:
- 공지사항과 VectorStore 검색 문서가 저장되어 있다.

When:
- 사용자가 검색어를 입력한다.

Then:
- VectorStore 유사도 검색 결과를 기준으로 관련 공지가 반환된다.
- 제목/본문 키워드 포함 여부가 보조 점수로 반영된다.

### Scenario C. 공지 요약
Given:
- 조회 대상 공지사항이 저장되어 있다.

When:
- 사용자가 요약 API를 호출한다.

Then:
- 저장된 요약이 있으면 재사용한다.
- 저장된 요약이 없으면 AI 요약을 생성하고 저장한 뒤 반환한다.

## 4. Functional Requirements
- FR-1: 시스템은 외부 공지사항의 sourceId, sourceUrl, title, content, author, category, postedAt을 저장해야 한다.
- FR-2: 시스템은 공지 제목과 본문을 하나의 검색 텍스트로 Spring AI VectorStore에 저장해야 한다.
- FR-3: 시스템은 VectorStore 유사도 검색 결과를 관련도 순으로 반환해야 한다.
- FR-4: 시스템은 공지 요약을 생성하고 재요청 시 저장된 요약을 반환해야 한다.
- FR-5: 공지사항 동기화는 수동 API가 아니라 스케줄러로 실행되어야 한다.

## 5. Behavioral Rules

### 5.1 Preconditions
- 검색어는 blank일 수 없다.
- 요약 대상 공지사항은 저장되어 있어야 한다.

### 5.2 Postconditions
- 동기화 성공 후 공지 원문과 VectorStore 검색 문서가 저장된다.
- 요약 성공 후 summary와 summarizedAt이 저장된다.

### 5.3 Invariants
- 동일한 외부 sourceId는 하나의 공지사항으로만 저장된다.
- 공지 원문이 갱신되면 기존 요약은 무효화된다.

### 5.4 Forbidden Rules
- 외부 사용자 요청으로 공지 동기화를 실행할 수 없다.
- 외부 크롤링 실패를 성공 응답으로 숨길 수 없다.

## 6. External Contracts

### 6.1 API Contract
- `GET /official-notices/search`: 공지사항 의미 검색
- `GET /official-notices/{noticeId}`: 공지 상세 조회
- `POST /official-notices/{noticeId}/summary`: 공지 AI 요약

### 6.2 Integration Contract
- 외부 시스템: `https://computer.knu.ac.kr/bbs/board.php?bo_table=sub5_1`
- AI 제공자: Spring AI OpenAI `ChatModel`, `EmbeddingModel`
- 벡터 저장소: Spring AI Qdrant `QdrantVectorStore`
- 벡터 문서 metadata에는 noticeId, sourceId, sourceUrl, title, author, category, postedAt, crawledAt을 저장한다.
- 실패 시 `NOTICE_CRAWL_FAILED` 또는 `NOTICE_AI_FAILED`를 반환한다.

### 6.3 Persistence Contract
- `official_notice`: 외부 공지 원문과 요약 저장
- `official_notice.source_id`는 unique이다.

## 7. Test Criteria
- 스케줄러가 새 공지를 발견하면 DB 저장과 VectorStore 저장을 수행한다.
- 이미 저장된 공지는 스케줄 동기화에서 건너뛴다.
- 검색어가 공지 제목/본문과 관련 있으면 결과에 포함된다.
- 없는 공지를 조회하거나 요약하면 `NOTICE_NOT_FOUND`가 발생한다.

## 8. Open Questions
- 운영 Qdrant 인스턴스와 collection 초기화 정책을 확정해야 한다.
- 공지사항 첨부파일 본문 추출은 후속 이슈로 분리할지 결정해야 한다.
- 스케줄 주기와 운영 환경별 활성화 정책을 확정해야 한다.
