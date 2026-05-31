# Pinpoint 운영 초안

`sidowi`에 Pinpoint를 붙일 때 바로 참고할 수 있는 초안 파일 모음.
실제 서버 경로(`/opt/docker/...`)에 바로 복사하기 전에 값과 경로를 팀 환경에 맞게 한 번 더 확인한다.

## 파일 구성

| 파일 | 용도 |
|---|---|
| `docker-compose.pinpoint.example.yml` | `sidowi`에서 Pinpoint 서버 컴포넌트를 backend와 분리해 띄우기 위한 compose 초안 |
| `pinpoint.env.example` | `docker-compose.pinpoint.example.yml`에서 쓰는 Pinpoint 공용 환경변수 초안 |
| `comit.env.pinpoint.example` | `knu-cse-comit-server`에 Pinpoint agent를 붙일 때 `/opt/docker/env/comit.env`에 넣을 값 초안 |

## 사용 순서

1. [docker-compose.pinpoint.example.yml](./docker-compose.pinpoint.example.yml)을 기준으로 `/opt/docker/compose/docker-compose.pinpoint.yml`을 만든다.
2. [pinpoint.env.example](./pinpoint.env.example)을 기준으로 `/opt/docker/env/pinpoint.env`를 만든다.
3. [comit.env.pinpoint.example](./comit.env.pinpoint.example)을 참고해 `/opt/docker/env/comit.env`에 Pinpoint agent 주입값을 추가한다.
4. rollout 순서와 smoke test는 [sidowi-pinpoint-rollout.md](../sidowi-pinpoint-rollout.md)를 따른다.
