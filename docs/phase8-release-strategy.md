# Phase 8 Release Strategy

## 1. 결론

Phase 8의 production deploy는 `main` 최신 커밋 직배포가 아니라 immutable version tag 기준으로 수행한다.

목표 흐름:

```text
feature branch -> dev -> release/v1.0 -> main -> v1.0.0 tag -> production deploy
```

`main`은 운영 가능한 source of truth이고, 운영 서버는 `v1.0.0` 같은 tag를 기준으로 배포한다.

## 2. 브랜치 역할

### feature branch

역할:

- Phase 8 세부 작업 수행
- 작은 단위의 문서/설정/검증 작업 진행

예시:

```text
feat/phase8-first-deploy
feat/phase8-profile-config
feat/phase8-flyway-baseline
```

정책:

- 기능 또는 설정 변경은 PR로 통합한다.
- 운영 secret은 포함하지 않는다.
- 문서 작업과 코드 작업은 가능하면 커밋 단위를 분리한다.

### dev

역할:

- Phase 8 작업 통합 브랜치
- 여러 feature branch 결과를 먼저 모으는 브랜치

정책:

- backend test, frontend lint/build 검증 대상
- production deploy 대상은 아니다.
- dev에서 검증된 내용만 release branch로 넘긴다.

### release/v1.0

역할:

- v1.0 운영 배포 후보 브랜치
- prod-like 리허설과 안정화 작업 수행

정책:

- 새 기능 추가를 제한한다.
- 배포 차단 버그, 설정 누락, 문서 누락 수정에 집중한다.
- prod-like 리허설 결과를 기록한다.

### main

역할:

- 운영 가능한 source of truth
- release branch 검증 완료 후 병합 대상

정책:

- main 최신 커밋을 자동으로 production에 직배포하지 않는다.
- main 병합 후 immutable tag를 생성한다.
- main에는 운영 실행에 안전한 local/test/prod 설정 구조가 존재할 수 있다.

### version tag

역할:

- 실제 production deploy 단위

예시:

```text
v1.0.0
v1.0.1
```

정책:

- tag는 immutable로 취급한다.
- 같은 tag를 이동하거나 재사용하지 않는다.
- hotfix는 새 patch tag로 배포한다.

## 3. 릴리즈 흐름

1. feature branch에서 Phase 8 세부 작업을 수행한다.
2. feature branch를 dev에 통합한다.
3. dev에서 backend test, frontend lint/build, 필요 시 Docker image build 검증을 수행한다.
4. `release/v1.0` 브랜치를 생성한다.
5. release branch에서 prod-like 리허설을 수행한다.
6. release branch를 main에 병합한다.
7. main에서 `v1.0.0` tag를 생성한다.
8. 운영 서버는 `v1.0.0` tag 기준으로 배포한다.

## 4. main 브랜치 포함 기준

main에 포함 가능:

- 운영 실행에 안전한 source code
- local/test/prod profile 구조
- 테스트 코드
- placeholder 기반 예시 문서
- production build에서 비활성화되는 검증 코드

main에 포함 금지:

- secret 파일
- 실제 운영 비밀번호
- 실제 JWT secret
- 실제 DB password
- 실제 Redis password
- 운영 credential이 포함된 `.env`
- production build 또는 운영 라우팅에서 노출되는 `/dev` 검증 UI

중요:

- local/test 코드를 main에서 무조건 삭제하지 않는다.
- 운영 비활성화는 profile/env/secret/build 설정으로 해결한다.

## 5. Rollback 기준

Rollback은 tag 기준으로 수행한다.

예시:

```text
현재 운영: v1.0.1
rollback 대상: v1.0.0
```

주의:

- DB migration이 포함된 release는 rollback 가능성을 별도로 검토해야 한다.
- irreversible migration은 운영 반영 전 명확히 표시한다.
- Redis 데이터 유실 또는 token 무효화 가능성은 사용자 영향으로 기록한다.

## 6. 이번 Step에서 하지 않는 작업

Phase 8-1에서는 다음 작업을 하지 않는다.

- 브랜치 생성 또는 병합 자동화
- tag 생성
- release branch 생성
- GitHub Actions workflow 작성
- production deploy
- Dockerfile 작성
- Nginx 설정 작성
- profile 파일 생성
- secret 파일 조회 또는 출력

