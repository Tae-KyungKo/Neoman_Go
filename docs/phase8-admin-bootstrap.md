# Phase 8-8 ADMIN Bootstrap

## Purpose

Phase 8-8 provides a controlled one-time path for creating the first ADMIN account without Flyway seed data, manual DB inserts, or a public ADMIN creation API.

## Runtime Configuration

```text
app.admin-bootstrap.enabled=${ADMIN_BOOTSTRAP_ENABLED:false}
app.admin-bootstrap.login-id=${ADMIN_BOOTSTRAP_LOGIN_ID:}
app.admin-bootstrap.email=${ADMIN_BOOTSTRAP_EMAIL:}
app.admin-bootstrap.password=${ADMIN_BOOTSTRAP_PASSWORD:}
app.admin-bootstrap.nickname=${ADMIN_BOOTSTRAP_NICKNAME:}
```

Default is disabled. Enable it only during the initial controlled bootstrap run.

## Behavior

- `enabled=false`: no account is created.
- Missing loginId, email, password, or nickname: startup fails.
- loginId, email, and nickname follow the Phase 9 user policy.
- Reserved nickname such as `관리자`, `운영자`, or `admin`: startup fails.
- `ADMIN_BOOTSTRAP_LOGIN_ID` already ADMIN: skip.
- `ADMIN_BOOTSTRAP_EMAIL` already ADMIN: skip.
- `ADMIN_BOOTSTRAP_LOGIN_ID` or `ADMIN_BOOTSTRAP_EMAIL` existing USER: fail; no automatic promotion.
- Any other ADMIN already exists: skip.
- No ADMIN exists and loginId, email, and nickname are unused: create ACTIVE ADMIN with an encoded password.

## Security Rules

- Do not add ADMIN seed rows to Flyway migrations.
- Do not expose a public ADMIN creation API.
- Do not allow normal signup to create ADMIN.
- Do not log ADMIN password, JWT secret, DB password, Redis password, AWS credential, or token values.
- After a successful run, disable `ADMIN_BOOTSTRAP_ENABLED` and remove `ADMIN_BOOTSTRAP_PASSWORD` from the runtime environment.

## Verification

Run:

```text
.\gradlew.bat compileJava
.\gradlew.bat test
git diff --check
```
