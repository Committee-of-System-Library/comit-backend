# Sidowi nginx 프록시 설정 구조 가이드

`sidowi` 서버의 nginx 리버스 프록시(`proxy` 컨테이너) 설정이 **어느 디렉터리에 있고, 어떻게 수정·반영하는지**를 정리한다.

> ⚠️ **가장 중요한 한 줄**
> `proxy` 컨테이너가 실제로 읽는 설정은 **`/00_maintenance/0_proxy/`** 다.
> `/opt/docker/nginx/` 는 **마운트되지 않는 죽은 트리**다. 여기를 수정하면 `nginx -t` 는 통과해도 **운영에 절대 반영되지 않는다.**

---

## TL;DR

| 경로 | 정체 | 수정해도 되나 |
| --- | --- | --- |
| `/00_maintenance/0_proxy/` | **라이브.** `proxy` 컨테이너에 `/etc/nginx` (ro) 로 마운트됨 | ✅ 여기를 수정 |
| `/opt/docker/nginx/` | **죽은 트리.** 어떤 컨테이너에도 마운트되지 않음 (upstream 이름도 옛값으로 갈라져 있음) | ❌ 절대 금지 |

두 트리는 이미 내용이 갈라져 있어서(예: upstream 이름 `knu-cse-comit-server:8080` vs 실제 `comit-backend`) 서로 동기화되지 않는다. **반드시 라이브 트리만 수정**한다.

---

## 실제 설정 구조

### 마운트

```text
host:/00_maintenance/0_proxy   →   container(proxy):/etc/nginx   (read-only)
```

확인:

```bash
docker inspect proxy --format '{{range .Mounts}}{{.Source}} -> {{.Destination}} ({{.Mode}}){{println}}{{end}}'
# /00_maintenance/0_proxy -> /etc/nginx (ro)
```

### include 체인

`comit` 라우팅은 `chcse.knu.ac.kr` 443 server 블록 안에서 include 된다.

```text
nginx.conf
└─ include conf.d/chcse.knu.ac.kr.conf
   └─ server { listen 443; server_name chcse.knu.ac.kr;
        include conf.d/appfn.conf
        include conf.d/comit.conf      ← comit 라우팅 (여기를 수정)
        include conf.d/ledger.conf
        include conf.d/locker.conf
        location / { proxy_pass http://comit-frontend; }   ← fallback (프론트)
      }
```

핵심: `comit.conf` 가 `location /`(프론트 fallback) **앞에** include 되므로,
`comit.conf` 안의 `location = ...`(exact) 또는 `location ^~ ...`(prefix) 블록이 우선 매칭된다.

### 실제 upstream 이름 (라이브 기준)

- `comit-backend` (백엔드, 8080)
- `comit-frontend` (프론트)
- `comit-loadtest:22500` (부하테스트)

---

## 수정·반영 방법

설정 파일은 `root` 소유이고, 운영 계정은 `sudo` 비밀번호가 필요해 비대화형으로 직접 쓸 수 없다.
대신 계정이 **`docker` 그룹**에 속하므로, **root 권한 컨테이너를 경유**해 호스트 파일을 수정한다.

### 1) 백업 + 교체

새 설정을 홈 디렉터리에 `~/comit.conf.new` 로 올려둔 뒤:

```bash
docker run --rm \
  -v /00_maintenance/0_proxy/conf.d:/conf \
  -v "$HOME":/src \
  nginx:latest sh -c '
    cp -a /conf/comit.conf /conf/comit.conf.bak-$(date +%Y%m%d%H%M%S) &&
    cp /src/comit.conf.new /conf/comit.conf &&
    chmod 644 /conf/comit.conf
  '
```

> 파일 내용에 `$time_iso8601` 같은 nginx 변수가 있으면, ssh→docker→sh 다단계에서 셸이 변수를 먹어버리지 않도록
> 인라인 `echo`/heredoc 대신 **파일을 통째로 올린 뒤 `cp`** 하거나 `base64` 로 전송한다.

### 2) 문법 검사 + 무중단 리로드

```bash
docker exec proxy nginx -t        # 문법 검사 (라이브 트리 기준)
docker exec proxy nginx -s reload # 무중단 반영
```

---

## 시각 엔드포인트: `/comit-staging/time`

선착순·카운트다운에서 클라이언트 로컬 시계 오차를 보정하기 위한 서버 시각 엔드포인트.
nginx가 백엔드를 거치지 않고 직접 응답한다.

```nginx
# conf.d/comit.conf
location = /comit-staging/time {
    default_type application/json;
    add_header Cache-Control "no-store" always;
    return 200 '{"server_time":"$time_iso8601","epoch":$msec}';
}
```

```bash
curl -s https://chcse.knu.ac.kr/comit-staging/time
# {"server_time":"2026-06-03T21:28:03+09:00","epoch":1780489683.056}
```

- `$time_iso8601` : ISO8601 (컨테이너 `TZ=Asia/Seoul` → `+09:00` 포함). 사람이 읽기/로깅용.
- `$msec` : epoch **초**(밀리초 정밀도). nginx 설정에서는 곱셈이 안 되므로 ms 변환은 클라이언트 몫(`epoch * 1000`).
- 클라이언트 유틸: `knu-cse-comit-client` 의 `src/shared/lib/server-time.ts` (`syncServerTime` / `serverNow`), `useServerTime` 훅.

---

## 트러블슈팅

**증상: `comit.conf` 를 고치고 `nginx -t` 도 통과하는데 변경이 반영되지 않는다.**

1. **죽은 트리를 고쳤을 가능성** (가장 흔함). `/opt/docker/nginx/` 가 아니라 `/00_maintenance/0_proxy/` 를 수정했는지 확인.
   - 컨테이너가 실제로 보는 파일과 비교: `docker exec proxy cat /etc/nginx/conf.d/comit.conf`
2. 리로드를 안 했을 가능성: `docker exec proxy nginx -s reload`.
3. `location` 우선순위: exact(`=`) > `^~` > 정규식 > prefix(`/`). 의도한 블록이 `location /` 보다 먼저 include 되는지 확인.

> 참고: `docker exec proxy nginx -t` 는 **라이브 트리**(`/00_maintenance/0_proxy`)만 검사한다.
> 죽은 트리를 수정하면 검사 대상에 포함되지 않으므로 항상 "통과"로 보이는 착시가 생긴다 — 이게 흔한 함정이다.
