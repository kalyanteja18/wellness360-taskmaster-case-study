# Application-side changes (already applied in this repo)

The original `taskmaster` repo didn't ship with Actuator or Prometheus
metrics support. Both are wired up here — this documents what was added
and why, for the write-up/presentation to the reviewers.

## 1. Dependencies added to `pom.xml`

```xml
<!-- Actuator: powers the ALB health check at /actuator/health -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<!-- Exposes /actuator/prometheus for Prometheus scraping -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

## 2. Configuration added to `application.properties`

```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true
```

DB connection values (`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`) are read
from environment variables, defaulting to an in-memory H2 DB for local dev
so the app still runs without AWS credentials on a laptop. In production,
ECS injects the real MySQL/Secrets Manager values (see `terraform/ecs.tf`).

## Why this matters

- The ALB target group health check (`terraform/alb.tf`) polls
  `/actuator/health`. Without Actuator, every ECS task would be marked
  unhealthy and the service would keep cycling tasks forever.
- Prometheus (`monitoring/prometheus.yml`) scrapes `/actuator/prometheus`.
  Without `micrometer-registry-prometheus`, that endpoint 404s and the
  Grafana dashboard shows no data.

Both changes are additive only — no existing business logic (task CRUD
endpoints, JPA entities) was touched.
