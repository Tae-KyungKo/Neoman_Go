# Production Deployment Guide

## Scope

This guide prepares the first production backend deployment for Neoman-Go. It does not create AWS resources automatically and does not connect to any production server.

Target topology:

- Backend: EC2 + Docker + host Nginx
- Backend API domain: `https://api.neomango.kr`
- Frontend canonical domain: `https://neomango.kr`
- `https://www.neomango.kr` redirects to `https://neomango.kr`
- Database: RDS MySQL
- Redis first deployment: EC2 Docker Redis with AOF
- TLS: Nginx terminates HTTPS for `api.neomango.kr`

## 1. DNS Preparation

1. Create a Route 53 hosted zone for `neomango.kr`.
2. Copy Route 53 name servers.
3. In the domain registrar, replace the default name servers with the Route 53 NS values.
4. Wait for DNS delegation propagation.
5. Create DNS records after AWS targets are ready:
   - `api.neomango.kr` to backend Elastic IP or load balancer target
   - `neomango.kr` to frontend CloudFront distribution
   - `www.neomango.kr` redirect target through frontend routing

Do not put AWS credentials in repository files.

## 2. AWS Resource Checklist

Create resources manually or through a reviewed infrastructure process:

- EC2 for backend
- Elastic IP for backend EC2
- RDS MySQL
- S3 bucket for frontend artifacts
- CloudFront distribution for frontend
- ACM certificate for frontend CloudFront
- Route 53 records

Backend TLS for `api.neomango.kr` can use Certbot on EC2 in the first deployment.

## 3. Security Groups

Backend EC2 inbound:

- `80/tcp` from internet for ACME redirect and HTTP redirect
- `443/tcp` from internet for API HTTPS
- SSH only from a restricted admin IP range

Backend EC2 outbound:

- RDS MySQL port to RDS security group
- HTTPS for package/image downloads when needed

RDS inbound:

- MySQL only from backend EC2 security group

Redis:

- First deployment Redis runs inside Docker on EC2.
- Do not expose `6379` to the internet.
- Redis must require a password.

## 4. EC2 Runtime Setup

Install on the backend EC2 host:

- Docker
- Docker Compose plugin
- Nginx
- Certbot
- Git

Keep application logs on stdout. Use `docker logs`, `docker compose logs`, and Nginx logs for the first operation phase.

## 5. Backend Release Checkout

Use immutable release tags for production deployment.

```bash
git clone <backend-repository-url> neomango-backend
cd neomango-backend
git fetch --tags
git checkout v1.0.0
```

Do not deploy from an untagged moving branch unless an emergency procedure explicitly approves it.

## 6. Production Environment File

Create `.env.prod` on the production server from `.env.prod.example`.

```bash
cp .env.prod.example .env.prod
chmod 600 .env.prod
```

Replace placeholders locally on the server. Do not commit `.env.prod`.

Required categories:

- `SPRING_PROFILES_ACTIVE=prod`
- RDS MySQL JDBC URL and username/password
- Redis host/port/password
- JWT secret
- CORS allowed origins
- ADMIN bootstrap values only during the first controlled run

After ADMIN creation:

- Set `ADMIN_BOOTSTRAP_ENABLED=false`
- Remove `ADMIN_BOOTSTRAP_PASSWORD`
- Restart backend

## 7. Production Compose

The production compose file is:

```text
docker-compose.prod.yml
```

It contains:

- backend container
- Redis container

It intentionally does not contain MySQL because production DB is RDS.

Redis policy:

- no public host port
- password required
- `appendonly yes`
- `appendfsync everysec`
- `maxmemory-policy noeviction`
- named volume for `/data`

Start:

```bash
docker compose -f docker-compose.prod.yml up --build -d
docker compose -f docker-compose.prod.yml ps
```

## 8. Nginx And TLS

Use the template:

```text
nginx/prod/api.neomango.kr.conf.template
```

Install it on the EC2 host after replacing certificate paths if needed.

Expected behavior:

- HTTP `80` redirects to HTTPS
- HTTPS `443` proxies to backend `127.0.0.1:8080`
- `/api/notifications/stream` uses SSE-specific proxy settings
- `/actuator/health` and `/actuator/info` are reachable
- token query parameters remain prohibited

Validate:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Issue TLS certificate with Certbot after DNS points to the EC2 host.

## 9. Health Checks

After backend and Nginx are running:

```bash
curl -i https://api.neomango.kr/actuator/health
curl -i https://api.neomango.kr/actuator/info
```

Expected:

- `/actuator/health`: HTTP 200 and `{"status":"UP"}`
- `/actuator/info`: HTTP 200
- no DB URL, Redis host, secret key, or detailed component output in public health

Sensitive actuator endpoints such as `/actuator/env`, `/actuator/beans`, and `/actuator/metrics` must not expose content.

## 10. First ADMIN Bootstrap

Use ADMIN bootstrap only once on the first production server.

1. Set `ADMIN_BOOTSTRAP_ENABLED=true`.
2. Set `ADMIN_BOOTSTRAP_LOGIN_ID`, `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD`, `ADMIN_BOOTSTRAP_NICKNAME`.
3. Start backend and confirm ADMIN creation from logs without printing password.
4. Set `ADMIN_BOOTSTRAP_ENABLED=false`.
5. Remove `ADMIN_BOOTSTRAP_PASSWORD`.
6. Restart backend.

Never create ADMIN through public signup. Do not document or commit real ADMIN passwords.

## 11. Smoke Test Order

1. `GET /actuator/health`
2. `GET /actuator/info`
3. `GET /api/notices`
4. Login with a controlled test account
5. Authenticated `GET /api/notifications`
6. Unauthenticated SSE should return `401`
7. Authenticated SSE should return `text/event-stream`
8. Verify CORS from `https://neomango.kr`

Do not print tokens in reports.

## 12. What Not To Do

- Do not commit `.env.prod`.
- Do not store production secrets in docs.
- Do not expose Redis `6379` publicly.
- Do not use Hibernate `ddl-auto=create/update` in production.
- Do not add Flyway seed data for ADMIN.
- Do not deploy from an unreviewed branch.
- Do not expose Actuator env, beans, metrics, configprops, heapdump, threaddump, or loggers endpoints.
