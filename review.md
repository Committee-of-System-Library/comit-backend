### ✨ 구현한 기능
- resolves #50

# 배경
OSIV가 false로 설정된 환경에서 불필요한 트랜잭션 사용이 DB 커넥션 점유 시간을 늘리고 있었습니다.
특히 클래스 레벨 @Transactional(readOnly = true)로 인해 write 메서드가 의도치 않게 readOnly 트랜잭션을 상속받거나, 단순 조회 메서드에도 불필요한 트랜잭션이 열리는 문제가 있었습니다.

# 변경 사항
1. 클래스 레벨 @Transactional 제거

의도치 않은 트랜잭션 상속을 막기 위해 클래스 레벨 선언을 제거하고 메서드 단위로 명시합니다.


2. @Transactional(readOnly = true) 제거

단건 조회의 경우 SimpleJpaRepository 내부에 이미 @Transactional(readOnly = true)가 선언되어 있어 서비스 레벨에서 별도로 감싸지 않아도 트랜잭션이 보장됩니다. 오히려 서비스 레벨에서 추가하면 레포지토리 트랜잭션이 서비스 트랜잭션에 합류하면서 카카오페이 기술 블로그 실측 기준 set autocommit, SET SESSION TRANSACTION READ ONLY, commit 등 최대 6개의 추가 DB 네트워크 요청만 발생합니다. 현재 코드베이스의 조회 메서드 대부분이 단건 조회였기 때문에 Write/Read DB가 분리되지 않은 환경에서는 실익이 없다고 판단했습니다.


3. 트랜잭션 필요 기준 정립

트랜잭션은 아래 경우에만 명시합니다.

여러 write 작업의 원자성이 필요한 경우
dirty checking으로 update가 발생하는 경우
엔티티 로드와 저장을 같은 영속성 컨텍스트에서 처리해야 하는 경우
단건 save()는 SimpleJpaRepository 내부 트랜잭션으로 충분합니다.


4. CommentQueryService 제거 및 서비스 간 의존 구조 개선

Write 서비스가 다른 서비스에 의존하는 구조는 어느 서비스가 어느 서비스를 알아야 하는지 기준이 모호해지고 순환 참조 위험이 있습니다. Write/Read DB 분리도 없는 상황에서 서비스 레이어만 CQRS 구조로 나누는 것은 복잡성만 높인다고 판단했습니다. Repository는 도메인 경계가 명확하기 때문에 각 서비스가 필요한 Repository를 직접 의존하도록 변경했습니다. 단위 테스트에서 목 체인이 줄어드는 이점도 있습니다.



### 📢 논의하고 싶은 내용
# 테스트 코드 전략 개선 제안

현재 문제
현재 서비스 단위 테스트는 대부분 Mock 객체 기반으로 작성되어 있습니다.

1. 신뢰도 문제

given()으로 설정한 행위는 실제 구현체가 그렇게 동작하길 기대하는 것일 뿐, 실제 동작을 보장하지 않습니다. Mock과 실제 구현체 사이의 괴리가 생겨도 테스트는 통과합니다. ([참고](https://medium.com/@chanhyeonglee/mock-%EA%B0%9D%EC%B2%B4-%EB%82%A8%EC%9A%A9%EC%9D%80-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%A7%9D%EC%B9%9C%EB%8B%A4-f38129e5d40a))

2. 유지보수 문제

내부 구현이 바뀌면 Mock 설정도 함께 바꿔야 합니다. 이번 작업에서 CommentQueryService 제거만으로 PostServiceTest, ReportServiceTest, CommentServiceTest, ComitApplicationTests 전부 손봐야 했던 것이 대표적인 예입니다.

제안
Repository는 실제 빈 주입 — @DataJpaTest 또는 @SpringBootTest로 실제 DB와 연동해 서비스를 테스트
Mock은 외부 API, S3 등 실제 구현이 불가능한 의존성에만 한정

구현 세부사항이 아닌 비즈니스 동작을 검증하게 되어 내부 구현이 바뀌어도 테스트가 깨지지 않습니다.

Mock 기반 테스트가 빠른 대신 신뢰도를 잃는 트레이드오프보다, 약간의 속도를 감수하고 실제 동작을 검증하는 것이 장기적으로 유지보수 비용이 낮다고 생각합니다.

### 🎸 기타
- [카카오페이 기술 블로그 - 단건 요청에 대해서는 Transaction 제거하기](https://tech.kakaopay.com/post/jpa-transactional-bri/#1-%EB%8B%A8%EA%B1%B4-%EC%9A%94%EC%B2%AD%EC%97%90-%EB%8C%80%ED%95%B4%EC%84%9C%EB%8A%94-transaction-%EC%A0%9C%EA%B1%B0%ED%95%98%EA%B8%B0)
- [테스트 코드](https://medium.com/@chanhyeonglee/mock-%EA%B0%9D%EC%B2%B4-%EB%82%A8%EC%9A%A9%EC%9D%80-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%A7%9D%EC%B9%9C%EB%8B%A4-f38129e5d40a)


<!-- This is an auto-generated comment: release notes by coderabbit.ai -->

## Summary by CodeRabbit

* **Refactor**
    * Decoupled service architecture by replacing internal service dependencies with direct repository layer access for improved separation of concerns across comment, post, and report handling
    * Reorganized image upload infrastructure for enhanced component modularity
    * Optimized transaction management strategies to improve performance and code clarity across service layer implementations

<!-- end of auto-generated comment: release notes by coderabbit.ai -->