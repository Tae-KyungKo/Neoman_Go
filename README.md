# 🎮 너만고 (Neoman-Go)
> [cite_start]**대학생을 위한 게임 및 스포츠 팀 빌딩 & 매치 메이킹 플랫폼** [cite: 11]

[cite_start]본 프로젝트는 인원이 부족한 대학생들이 게임(LoL, 발로란트 등)이나 스포츠(축구, 농구 등)를 함께 즐길 팀원을 찾고 매칭할 수 있도록 돕는 백엔드 중심의 웹 서비스입니다[cite: 11].

---

## 🚀 Project Overview
- [cite_start]**개발 기간**: 2026.04.26 ~ (6개월+ 목표) [cite: 6]
- [cite_start]**주요 목표**: 실무 수준의 아키텍처 설계, 동시성 제어, 분산 환경 고려 및 관측성 확보 [cite: 68, 110]
- [cite_start]**핵심 기술**: Java 17, Spring Boot 3.x, MySQL 8.0, Redis, MongoDB, Docker [cite: 69, 71, 110]

## 🛠 Tech Stack
### Backend
- [cite_start]**Core**: Java 17, Spring Boot 3.x, Spring Data JPA 
- [cite_start]**Database**: MySQL 8.0 (Main), MongoDB (Chat Log) [cite: 92]
- [cite_start]**Cache & Concurrency**: Redis (JWT Management, Distributed Lock) [cite: 71, 128]
- [cite_start]**Communication**: SSE (Real-time Notification), WebSocket (Team Chat) [cite: 132]

### Infrastructure & DevOps
- [cite_start]**Container**: Docker, Nginx (Load Balancing) [cite: 89, 110]
- [cite_start]**CI/CD**: GitHub Actions [cite: 89]
- [cite_start]**Monitoring**: Prometheus, Grafana [cite: 110]
- [cite_start]**Cloud**: AWS (EC2, S3, RDS) [cite: 80, 89]

## 📌 Key Features
1. [cite_start]**인증 및 계정**: JWT 기반 무상태 인증, 이메일 인증을 통한 회원가입 [cite: 61, 69]
2. [cite_start]**팀 관리**: 카테고리별 팀 생성 및 가입 신청, 주장 권한 위임 기능 [cite: 52]
3. [cite_start]**매치 메이킹**: 매치 보드 등록 및 신청, 결과 저장 시스템 [cite: 55]
4. [cite_start]**실시간 소통**: SSE 기반 활동 알림 및 WebSocket 활용 팀별 실시간 채팅 [cite: 52, 132]
5. [cite_start]**관리자 시스템**: 회원 제재, 게시글 관리 및 시스템 통계 대시보드 [cite: 112, 114]

## 🏗 Architecture (Planned)
- [cite_start]**Stateless Architecture**: 서버 확장성을 고려한 JWT 및 Redis 활용 [cite: 71, 110]
- [cite_start]**Concurrency Control**: 팀 가입 신청 시 선착순 처리를 위한 분산 락 적용 [cite: 128]
- [cite_start]**Observability**: Prometheus & Grafana를 활용한 API 성능(500ms 이내) 모니터링 [cite: 110]

---

## 📝 Git Commit Convention
실무 표준인 **Conventional Commits** 규칙을 준수합니다.

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
