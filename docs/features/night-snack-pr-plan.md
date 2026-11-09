# 야식마차 변경 PR 분리 계획

관련 이슈: #201. 기존 완성본은 backup/night-snack-complete-20261109에 보존한다.

## 브랜치와 리뷰 순서

| 순서 | head | base | 리뷰 범위 |
|---|---|---|---|
| 통합 초안 | feat/#201-night-snack | main | 단계별 작업 계획. 하위 PR 반영 후 최종 동작 검증 |
| 1 | feat/#201-night-snack-insert | feat/#201-night-snack | INSERT 전용 Writer, 공유 게이트 조회와 테스트 |
| 2 | feat/#201-night-snack-selection | feat/#201-night-snack-insert | 선정 enum, ID 하한·즉시 확정 구간과 V115 |
| 3 | feat/#201-night-snack-scheduler | feat/#201-night-snack-selection | 선정 상태 저장·API, 스케줄러 연결, 기존 전략 제거와 V116 |
| 4 | docs/#201-night-snack-manual-test | feat/#201-night-snack-scheduler | 로컬 실행과 수동 검증 가이드 |

각 PR은 직전 브랜치만 base로 삼아 해당 단계의 diff만 리뷰한다.
다음 PR은 앞 PR을 포함하므로 순서대로 통합한다. 앞 PR을 통합 브랜치에 머지한 뒤 다음 PR의 base를
통합 브랜치로 바꾼다. squash로 커밋 ID가 바뀌었다면 다음 브랜치를 재정렬한 뒤 변경 diff를 확인한다.
모든 단계가 통합 브랜치에 반영되고 최종 검증이 끝난 뒤 main 대상 통합 PR을 머지한다.

## 단계별 동작

1. INSERT Writer는 기존 신청 서비스와 별도 빈으로 추가한다. 기존 요청 경로는 유지한다.
   정원 밖 요청도 접수하는 INSERT 처리만 독립적으로 리뷰하고 실제 MySQL에서 검증한다.
2. 선정 enum과 행사 ID 기준점만 준비한다. 기존 open()과 카운터 기반 경로를 유지한다.
   신규 open(idBase)은 후속 스케줄러가 사용한다. 선정 상태의 신청 엔티티 매핑과 API 전환은 3단계다.
3. 신청을 INSERT 경로에 연결하고 정원의 최대 50%를 즉시 확정한다. 나머지는 PENDING으로 조회하며,
   스케줄러가 ID 순으로 최종 정원만큼 선정한다. 수령 상태와 당락은 구분한다.
4. 로컬 실행·계정 준비·신청·폴링·SQL 확인 절차를 제공한다.

## 마이그레이션 경계

V115는 ID 기준점과 집계 인덱스를 추가한다. V116은 선정 상태 추가와 일반 remaining 컬럼 삭제를 함께 수행한다.
이미 적용했을 수 있는 V116의 SQL은 수정하지 않는다. 따라서 기존 카운터 제거와 새 실행 경로 활성화는
V116을 추가하는 3단계에서 함께 처리한다. 스케줄러 연결과 카운터 제거를 별도 PR로 분리하지 않는 이유다.

## 검증

- 1단계: INSERT Writer 단위 테스트와 실제 MySQL의 정원 초과 접수 테스트, 기존 기능 회귀 테스트
- 2단계: 선정 구간 경계와 기존 카운터 경로 회귀 테스트, V115 포함 MySQL 기동 검증
- 3단계: 동시 신청, 중복, 지연 커밋·마감 경합, ID 공백, 예약분, 시간 마감, 롤백 복구와 API 계약 검증
- 4단계: 문서의 셸·Python·JSON 구문과 로컬 링크 검증
- 각 코드 단계: 전체 테스트 소스 컴파일과 generateApiDocs 성공 확인

이 문서는 PR 구성 계획이다. 1·2단계만으로 새 신청 방식을 서비스에 활성화하지 않는다.
