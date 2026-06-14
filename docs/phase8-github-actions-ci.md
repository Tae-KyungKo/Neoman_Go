# Phase 8-9 Backend GitHub Actions CI

## Scope

Phase 8-9 adds backend CI only. It does not deploy the application.

## Workflow

Workflow file:

```text
.github/workflows/backend-ci.yml
```

Triggers:

- `pull_request` to `dev`, `main`, `release/**`
- `push` to `dev`, `main`, `release/**`
- `workflow_dispatch`

## Runtime

- Runner: `ubuntu-latest`
- Java: Temurin 17
- Gradle cache: enabled through `actions/setup-java`
- Spring profile: `test`
- ADMIN bootstrap: disabled with `ADMIN_BOOTSTRAP_ENABLED=false`

## Redis

CI uses a GitHub Actions Redis service container:

- Image: `redis:7-alpine`
- Port: `6379:6379`
- Password: none

This is CI-only Redis and is unrelated to production Redis credentials.

## Verification Commands

The workflow runs:

```text
./gradlew --no-daemon compileJava
./gradlew --no-daemon test
docker build -t neomango-backend-ci .
```

The Docker image is built only for verification. It is not pushed.

## Explicit Exclusions

- No automatic deployment
- No GitHub Secrets requirement
- No AWS CLI, ECR push, EC2 access, SSH, SCP, or Docker image push
- No frontend workflow
- No Actuator health endpoint
- No `.env.prodlike` or secret file usage

Branch protection should be configured later in the GitHub UI after the workflow is confirmed.
