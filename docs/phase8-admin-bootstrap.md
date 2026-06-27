# Phase 8-8 ADMIN Bootstrap

## Purpose

Phase 8-8 provides a controlled one-time path for creating the first ADMIN account without Flyway seed data, manual DB inserts, or a public ADMIN creation API.

## Runtime Configuration

```text
app.admin-bootstrap.enabled=${ADMIN_BOOTSTRAP_ENABLED:false}
app.admin-bootstrap.email=${ADMIN_EMAIL:}
app.admin-bootstrap.password=${ADMIN_PASSWORD:}
app.admin-bootstrap.nickname=${ADMIN_NICKNAME:}
```

Default is disabled. Enable it only during the initial controlled bootstrap run.

## Behavior

- `enabled=false`: no account is created.
- Missing email, password, or nickname: startup fails.
- Email, password, and nickname follow the existing signup validation policy.
- `ADMIN_EMAIL` already ADMIN: skip.
- `ADMIN_EMAIL` existing USER: fail; no automatic promotion.
- Any other ADMIN already exists: skip.
- No ADMIN exists and email is unused: create ACTIVE ADMIN with an encoded password.

## Security Rules

- Do not add ADMIN seed rows to Flyway migrations.
- Do not expose a public ADMIN creation API.
- Do not allow normal signup to create ADMIN.
- Do not log ADMIN password, JWT secret, DB password, Redis password, AWS credential, or token values.
- After a successful run, disable `ADMIN_BOOTSTRAP_ENABLED` and remove `ADMIN_PASSWORD` from the runtime environment.

## Verification

Run:

```text
.\gradlew.bat compileJava
.\gradlew.bat test
git diff --check
```
