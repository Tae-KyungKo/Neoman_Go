# 🎮 너만고 (Neoman-Go)
> **대학생을 위한 게임 및 스포츠 팀 빌딩 & 매치 메이킹 플랫폼**

본 프로젝트는 인원이 부족한 대학생들이 게임(LoL, 발로란트 등)이나 스포츠(축구, 농구 등)를 함께 즐길 팀원을 찾고 매칭할 수 있도록 돕는 백엔드 중심의 웹 서비스입니다.

---

## 🛠 Infrastructure & DevOps
* **Container**: Docker, Nginx (Load Balancing) 
* **CI/CD**: GitHub Actions
* **Monitoring**: Prometheus, Grafana
* **Cloud**: AWS (EC2, S3, RDS)

## 📌 Key Features
1. **인증 및 계정**: JWT 기반 무상태 인증, 이메일 인증을 통한 회원가입
2. **팀 관리**: 카테고리별 팀 생성 및 가입 신청, 주장 권한 위임 기능
3. **매치 메이킹**: 매치 보드 등록 및 신청, 결과 저장 시스템
4. **실시간 소통**: SSE 기반 활동 알림 및 WebSocket 활용 팀별 실시간 채팅
5. **관리자 시스템**: 회원 제재, 게시글 관리 및 시스템 통계 대시보드

## 🏗 Architecture (Planned)
* **Stateless Architecture**: 서버 확장성을 고려한 JWT 및 Redis 활용
* **Concurrency Control**: 중복 가입 신청과 중복 승인 방지를 위한 DB 제약조건 및 트랜잭션 전략 적용
* **Observability**: Prometheus & Grafana를 활용한 API 성능(500ms 이내) 모니터링

---

## 📝 Git Commit Convention
| Type | Description |
| :--- | :--- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (README 등) |
| `style` | 코드 포맷팅 (세미콜론 누락 등) |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 추가 |
| `chore` | 빌드 업무, 패키지 매니저 설정 등 |

**예시**: `feat(auth): 이메일 인증 기반 회원가입 로직 구현`

---

## 🧭 Issue Management Strategy

이슈는 단순 기능 목록이 아니라, 백엔드 정합성·보안·성능 리스크를 추적하는 단위로 관리한다.

### 1. 이슈 분리 기준
* 하나의 이슈는 하나의 도메인 흐름 또는 하나의 기술 기반을 완성하는 단위로 만든다.
* Controller, Service, Entity, Repository를 무조건 파일 단위로 쪼개지 않는다.
* 트랜잭션, 권한 검증, DB 제약조건, 동시성 처리가 함께 검토되어야 하는 기능은 하나의 이슈에서 다룬다.
* 구현 범위가 커지면 `1차 구현`, `정합성 보강`, `조회 최적화`, `테스트 보강`으로 나눈다.

### 2. 이슈 본문 필수 항목
* 배경: 왜 지금 필요한 작업인지 설명한다.
* 구현 범위: 이번 이슈에서 실제로 끝낼 범위를 명확히 적는다.
* 설계 고려사항: 트랜잭션, 권한, 동시성, N+1, DB 제약조건을 검토한다.
* 완료 기준: 코드 작성뿐 아니라 테스트, 예외 처리, 문서 반영 여부를 포함한다.
* 제외 범위: 이번 이슈에서 하지 않을 일을 명시해 범위가 커지는 것을 막는다.

### 3. 우선순위 전략
* P0: 프로젝트 기반 없이는 진행이 막히는 작업이다. 예: 공통 예외, 보안 기본 구조, 인증 기반.
* P1: 핵심 도메인 정합성에 직접 영향이 있는 작업이다. 예: Team, TeamMember, TeamApplication, 승인 동시성.
* P2: 사용자 경험 또는 운영 품질을 높이는 작업이다. 예: 알림, 모니터링, 관리자 기능.
* P3: 확장성 또는 리팩토링 중심 작업이다. 예: CQRS 분리, 이벤트 기반 구조 전환.
## Production Deployment Docs

Production deployment is tag-based and documented under `docs/`.

- [Production deployment guide](docs/production-deployment-guide.md)
- [Production runbook](docs/production-runbook.md)
- [Rollback guide](docs/rollback-guide.md)
- [Release checklist](docs/release-checklist.md)
- [Phase 8 release rehearsal](docs/phase8-release-rehearsal.md)

Real production secrets must stay outside Git. Use `.env.prod.example` as a placeholder template only.
