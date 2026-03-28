# Feature Toggle 설계 가이드

Feature Toggle(= Feature Flag)은 **코드를 재배포하지 않고 시스템 동작을 바꾸는 기법**입니다.
단순한 on/off 스위치처럼 보이지만, 목적에 따라 설계 방식과 수명이 완전히 달라집니다.

> 원본 아티클: [Feature Toggles — Martin Fowler](https://martinfowler.com/articles/feature-toggles.html)

---

## 우리 코드에 이미 있는 Feature Toggle

추상적인 개념보다 우리 코드에서 먼저 찾아보겠습니다.

```yaml
# application.yml
comit:
  auth:
    sso:
      enabled: ${COMIT_AUTH_SSO_ENABLED:false}   # ← Ops Toggle
    bridge:
      enabled: ${COMIT_AUTH_BRIDGE_ENABLED:false} # ← Release Toggle
```

- `sso.enabled` — 프로덕션에서 SSO 인증 필터를 켜고 끌 수 있는 **운영용 스위치**
- `bridge.enabled` — 로컬 개발 시에만 헤더 기반 인증을 활성화하는 **개발/릴리스 분리용 스위치**

이게 Feature Toggle의 가장 기본적인 형태입니다.

---

## Toggle 4종류 — 목적이 다르면 설계도 다르다

모든 flag를 `boolean` 하나로 똑같이 만들면 나중에 관리가 어려워집니다.
목적에 따라 **수명**, **동적 여부**, **코드 구조**가 달라져야 합니다.

### 1. Release Toggle — 배포와 공개를 분리한다

아직 완성되지 않았거나 공개 시점을 조절해야 하는 기능을 프로덕션에 미리 올려두는 용도입니다.

```
코드는 이미 서버에 올라가 있음
  → 스위치가 꺼져 있어서 사용자에게는 안 보임
  → 준비되면 스위치 켜기
```

**특징:**
- 수명이 짧아야 합니다. 기능이 공개되면 즉시 제거.
- 정적 설정(환경변수, YAML)으로 충분합니다.
- `comit.auth.bridge.enabled`가 이 패턴에 가깝습니다.

**언제 씁니까:**
새 API 응답 필드, 새 결제 플로우, 새 추천 로직을 미리 배포해두고 공개 시점만 조절할 때.

---

### 2. Experiment Toggle — A/B 테스트

어떤 구현이 더 나은지 비교하기 위해 사용자를 cohort로 나누는 용도입니다.

**특징:**
- 요청마다 사용자 문맥을 보고 판단해야 합니다 (동적).
- 통계적 유효성을 위해 같은 사용자에게 일관된 결과를 보장해야 합니다.
- 실험이 끝나면 제거하거나 승자 경로만 남깁니다.

**언제 씁니까:**
추천 알고리즘 비교, 버튼 문구 실험, 주문 플로우 개선 효과 측정.

---

### 3. Ops Toggle — 운영 안정성 스위치

장애나 고부하 상황에서 특정 기능을 즉시 끄는 kill switch입니다.

**특징:**
- 새 기능의 성능 영향이 불확실할 때 안전망으로 씁니다.
- 일부는 짧게 쓰고 제거하지만, 일부는 장기 kill switch로 남습니다.
- `comit.auth.sso.enabled`가 이 패턴에 해당합니다.

**언제 씁니까:**
"트래픽 폭주 시 추천 패널 끄기", "검색 확장 기능이 서버를 너무 먹으니 임시 비활성화".

---

### 4. Permissioning Toggle — 권한별 기능 제어

특정 사용자 집단에게만 기능을 허용하는 용도입니다.

**특징:**
- 항상 per-request 판단이 필요합니다 (동적).
- 유료 사용자 정책처럼 아주 오래 살아남을 수 있습니다.
- 임시 toggle이 아니라 **권한 정책**에 가까워질 수 있습니다.

**언제 씁니까:**
"이 기능은 운영진만", "이 기능은 유료 플랜만", "알파 기능은 베타 사용자만".

---

### 한눈에 비교

| 종류 | 수명 | 동적 여부 | 대표 예시 |
|------|------|-----------|-----------|
| Release Toggle | 단기 (기능 공개까지) | 정적 | `bridge.enabled` |
| Experiment Toggle | 중기 (실험 기간) | 동적 (사용자별) | A/B 테스트 |
| Ops Toggle | 가변 (장기 가능) | 정적 or 동적 | `sso.enabled`, kill switch |
| Permissioning Toggle | 장기 | 동적 (사용자별) | 유료 플랜, 관리자 전용 |

---

## 설계 원칙

### 원칙 1. 비즈니스 코드가 flag 이름을 직접 알게 하지 마라

**나쁜 구조** — 서비스 코드에 flag 이름이 퍼짐:

```java
// 서비스 곳곳에 이런 코드가 흩어짐
if (featureFlagService.isEnabled("new-payment-flow")) {
    processNewPayment(order);
} else {
    processLegacyPayment(order);
}
```

`"new-payment-flow"`라는 문자열이 비즈니스 로직에 직접 박히면, flag 이름이 바뀌거나 판단 로직이 복잡해질 때 수정 범위가 넓어집니다.

**나은 구조** — 결정 로직을 분리하고 주입:

```java
// 정책 객체로 결정을 캡슐화
public interface PaymentPolicy {
    boolean useNewFlow();
}

// 서비스는 정책만 받아서 사용
@Service
public class OrderService {
    private final PaymentPolicy paymentPolicy;

    public void processOrder(Order order) {
        if (paymentPolicy.useNewFlow()) {
            processNewPayment(order);
        } else {
            processLegacyPayment(order);
        }
    }
}

// 실제 flag 판단은 여기서만
@Component
public class FeatureFlagPaymentPolicy implements PaymentPolicy {
    private final FeatureFlags featureFlags;

    @Override
    public boolean useNewFlow() {
        return featureFlags.isEnabled("new-payment-flow");
    }
}

// 테스트에서는 간단히 교체
PaymentPolicy alwaysNewFlow = () -> true;
PaymentPolicy alwaysLegacy  = () -> false;
```

`OrderService`는 flag 시스템을 전혀 모릅니다. 테스트할 때 람다 하나로 경로를 고정할 수 있습니다.

---

### 원칙 2. 수명에 따라 코드 구조를 다르게

```
1~2주 뒤 제거할 Release Toggle  →  if/else 허용, 단 제거 일정 명시
오래 갈 Permissioning Toggle   →  전략 객체, 정책 클래스로 분리
장기 Ops Toggle (kill switch)  →  인프라 경계에 배치, 모니터링 연결
```

---

### 원칙 3. 정적으로 충분하면 동적으로 만들지 마라

실시간 제어 UI, 런타임 변경 API는 강력하지만 복잡하고 위험합니다.
환경변수나 YAML로 충분한 케이스가 생각보다 많습니다.

```
환경변수 / YAML   →  재배포 필요, 단순하고 안전
DB / 설정 서버    →  재배포 없이 변경, 복잡도 증가
런타임 API        →  즉시 변경, 운영 사고 위험도 높음
```

---

### 원칙 4. Toggle은 빚이다 — 만들 때 제거 계획까지 세워라

flag는 만들기 쉬워서 금방 쌓입니다. 방치하면:

- 코드 읽기 어려워짐
- ON/OFF 조합이 곱으로 늘어나 테스트 부담 증가
- "이 flag 아직 필요한가?" 아무도 모르는 상황

**권장 관행:**

```java
// toggle 추가 시 주석에 제거 조건과 시점을 명시
// TODO: [FEATURE-FLAG] new-payment-flow
//   제거 조건: 새 결제 플로우가 전체 배포 완료되면
//   제거 기한: 2026-04-30
//   담당: @bohyeong
if (paymentPolicy.useNewFlow()) { ... }
```

또는 backlog에 제거 티켓을 처음부터 같이 등록합니다.

---

## 테스트 전략

flag를 추가하는 순간 **기능 1개가 아니라 경로 2개 이상**이 생깁니다.
ON/OFF 두 경로를 모두 테스트해야 합니다.

```java
@Test
void 새_결제_플로우_활성화시_신규_로직_실행() {
    OrderService sut = new OrderService(() -> true); // new flow ON
    sut.processOrder(order);
    // 새 로직 검증
}

@Test
void 새_결제_플로우_비활성화시_기존_로직_실행() {
    OrderService sut = new OrderService(() -> false); // new flow OFF
    sut.processOrder(order);
    // 기존 로직 검증
}
```

정책 객체를 주입하는 구조로 만들면 테스트에서 flag 판단을 간단히 고정할 수 있습니다.

---

## 실천 체크리스트

- [ ] flag를 추가할 때 4종류 중 어느 것인지 먼저 결정한다
- [ ] 서비스 코드에 flag 이름 문자열을 직접 넣지 않는다
- [ ] 오래 갈 flag는 if/else 대신 정책/전략 클래스로 분리한다
- [ ] flag 추가 시 제거 조건과 기한을 주석 또는 티켓으로 남긴다
- [ ] ON/OFF 두 경로 테스트를 모두 작성한다
- [ ] "실시간 제어 UI"는 정말 필요할 때만 만든다

---

> 참고: [Feature Toggles (aka Feature Flags) — Pete Hodgson / Martin Fowler's Blog](https://martinfowler.com/articles/feature-toggles.html)
