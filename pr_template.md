### 관련 이슈

- resolve #111

---

## 설계 상세

### 1. 좋아요 2배 이벤트 — LocalTime 기반 multiplier

**핵심 의사결정: Redis flag 방식 대신 LocalTime 체크 채택**

Redis flag 방식은 이벤트 on/off를 즉시 반영할 수 있지만, Blue-Green 배포 시
신구 서버가 동일한 Redis key를 바라보면 배포 도중 이벤트 적용 여부가 불일치할 수 있음.
시간 기반 체크는 공유 상태가 없어 어떤 서버 인스턴스든 동일하게 동작하며, 추가 Redis I/O도 없음.

```java
// BoothLikeService.java
private int getLikeMultiplier() {
    LikeProperties.DoubleEvent event = likeProperties.doubleEvent();
    if (!event.enabled()) return 1;  // 이벤트 비활성화 시 즉시 반환

    LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul")); // 타임존 명시
    LocalTime start = LocalTime.parse(event.startTime());
    LocalTime end   = LocalTime.parse(event.endTime());

    return (!now.isBefore(start) && now.isBefore(end)) ? 2 : 1;
}
```

multiplier 계산만 별도 메서드로 분리하여 기존 `like()` 흐름을 변경하지 않음.

```java
// like() 호출부 — 기존 로직 변경 없음
int multiplier = getLikeMultiplier();
Double score = redisTemplate.opsForZSet().incrementScore(RANKING_KEY, String.valueOf(boothId), multiplier);
```

**env 외부화 — 재빌드 없이 이벤트 시간 변경**

```yaml
like:
  double-event:
    enabled: ${LIKE_DOUBLE_EVENT_ENABLED:false}
    start-time: ${LIKE_DOUBLE_EVENT_START:12:00}
    end-time: ${LIKE_DOUBLE_EVENT_END:14:00}
```

`@ConfigurationProperties` record를 사용해 타입 안전하게 바인딩.

```java
@ConfigurationProperties(prefix = "like")
public record LikeProperties(RateLimit rateLimit, DoubleEvent doubleEvent) {
    public record DoubleEvent(boolean enabled, String startTime, String endTime) {}
}
```

---

### 2. Rate Limiting — 필터 체인 3단계 구성

좋아요 도배 방지를 서비스 레이어가 아닌 **필터 레이어에서 차단**.
서비스 레이어까지 요청이 도달하기 전에 429를 반환하므로 DB/Redis 부하를 줄임.

```
Request
  │
  ▼
[Order 1] ClientIpFilter          — X-Forwarded-For에서 실제 IP 추출 → request attribute 저장
  │
  ▼
[Order 2] DeviceIdCookieFilter    — 쿠키에서 device_id 읽기, 없으면 UUID 발급 → request attribute 저장
  │
  ▼
[Order 3] LikeRateLimitFilter     — IP + DeviceId 조합으로 Rate Limit 판단, 초과 시 429 반환
  │
  ▼
Controller → BoothLikeService
```

**LikeRateLimiter — Redis increment 기반 슬라이딩 카운터**

```java
// LikeRateLimiter.java
public boolean isAllowed(String clientIp, String deviceId) {
    String key = "like:rate:%s:%s".formatted(clientIp, deviceId);

    long currentCount = Optional.ofNullable(redisTemplate.opsForValue().get(key))
        .map(Long::parseLong).orElse(0L);

    if (currentCount >= likeProperties.rateLimit().maxLikes()) return false;

    Long newCount = redisTemplate.opsForValue().increment(key);
    if (newCount == 1) {
        redisTemplate.expire(key, Duration.ofSeconds(likeProperties.rateLimit().ttlSeconds()));
    }
    return true;
}
```

key가 처음 생성될 때(`newCount == 1`)만 TTL을 설정해 window를 고정.

**LikeRateLimitFilter — /booths/*/likes 경로만 적용**

```java
// LikeRateLimitFilter.java
private boolean isLikesPath(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod())
        && request.getRequestURI().matches(".*/booths/[^/]+/likes$");
}
```

정규식으로 경로를 판단해 다른 API에는 필터가 개입하지 않음.
Rate Limit 초과 시 서비스 레이어 없이 필터에서 즉시 429 반환.

**FilterConfig — 필터 등록 및 쿠키 설정 외부화**

```java
// FilterConfig.java
@Value("${device-id.cookie.max-age}")   private int cookieMaxAge;
@Value("${device-id.cookie.same-site}") private String cookieSameSite;
@Value("${device-id.cookie.secure}")    private boolean cookieSecure;
```

쿠키 속성(max-age, SameSite, Secure)을 FilterConfig에서 주입받아
`DeviceIdCookieFilter`에 전달. 필터 내부에 설정을 하드코딩하지 않음.

---

## 테스트 시나리오

- [ ] 이벤트 시간대 내 좋아요 요청 → 점수가 2씩 증가하는지 확인
- [ ] 이벤트 시간대 외 좋아요 요청 → 점수가 1씩 증가하는지 확인
- [ ] `LIKE_DOUBLE_EVENT_ENABLED=false` → 시간대 내에도 1배로 적용되는지 확인
- [ ] Rate Limit 초과 요청 → HTTP 429 반환 확인
- [ ] Rate Limit 창(TTL) 지난 후 재요청 → 정상 응답 확인
- [ ] OPTIONS 요청 → DeviceId 쿠키 미발급 확인 (CORS preflight 오염 방지)

---

## 환경변수 설정 예시

```bash
# 이벤트 활성화 및 시간 설정
LIKE_DOUBLE_EVENT_ENABLED=true
LIKE_DOUBLE_EVENT_START=12:00
LIKE_DOUBLE_EVENT_END=14:00

# Rate Limit 설정
LIKE_RATE_LIMIT_TTL_SECONDS=10
LIKE_RATE_LIMIT_MAX_LIKES=5
```
