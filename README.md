# Taskmaster — AWS ECS Infrastructure Automation (Wellness360 Case Study)

Automated infrastructure provisioning and CI/CD pipeline for the
[`taskmaster`](https://github.com/nreddywellness360/taskmaster) Spring Boot
application, using Docker, ECR, ECS Fargate, Terraform, and GitHub Actions —
with Prometheus + Grafana monitoring.

## Architecture

```
                          Internet
                             │
                    ┌────────▼─────────┐
                    │  Application LB   │  (public subnets)
                    └────────┬─────────┘
                             │ :8080
              ┌──────────────┴──────────────┐
              │        ECS Fargate           │ (private subnets)
              │  ┌────────────────────────┐  │
              │  │  taskmaster-app x2      │  │
              │  │  (Spring Boot container)│  │
              │  └───────────┬────────────┘  │
              └──────────────┼───────────────┘
                              │
                    ┌─────────▼─────────┐
                    │   RDS MySQL 8.0    │  (private subnets)
                    └────────────────────┘

  Images pushed to ──► ECR
  Credentials stored in ──► Secrets Manager
  Logs shipped to ──► CloudWatch Logs
  Metrics scraped from ──► /actuator/prometheus ──► Prometheus ──► Grafana
```

**Key design decisions:**
- **Private subnets for ECS + RDS**: application and database are not
  directly internet-reachable; only the ALB sits in public subnets.
- **Secrets Manager over plaintext env vars**: DB credentials are generated
  randomly by Terraform and injected into the container at runtime.
- **Fargate over EC2**: no server patching/management overhead, fits a
  small-team case study well.
- **Single NAT Gateway**: cost-optimized for this exercise. A real
  production setup would use one NAT Gateway per AZ for high availability.

## Repository Structure

```
.
├── src/                     # Spring Boot Task Management REST API
│   ├── main/java/.../TaskmasterApplication.java
│   ├── main/java/.../controller/TaskController.java   # CRUD REST endpoints
│   ├── main/java/.../model/Task.java, TaskRepository.java
│   ├── main/resources/application.properties          # Actuator + DB config
│   └── test/java/.../TaskControllerIT.java             # Unit/integration tests
├── pom.xml                  # Adds Actuator + Micrometer-Prometheus to the
│                             # original taskmaster skeleton's dependencies
├── terraform/              # All infrastructure as code
│   ├── versions.tf         # Provider requirements
│   ├── variables.tf        # Configurable inputs
│   ├── vpc.tf               # VPC, subnets, NAT, routing
│   ├── security_groups.tf  # ALB + ECS + RDS security groups
│   ├── ecr.tf               # Container image registry
│   ├── rds.tf                # MySQL database + Secrets Manager
│   ├── alb.tf                # Load balancer + target group
│   ├── iam.tf                # ECS execution/task roles
│   ├── ecs.tf                # Cluster, task definition, service
│   └── outputs.tf            # ALB URL, ECR URL, etc.
├── .github/workflows/
│   └── deploy.yml            # CI/CD: test → build → push → deploy → smoke test
├── monitoring/
│   ├── prometheus.yml                    # Scrape config
│   ├── docker-compose.monitoring.yml     # Local Prometheus + Grafana stack
│   ├── grafana-provisioning/             # Auto-configured datasource + dashboard
│   └── grafana-dashboards/
│       └── taskmaster-dashboard.json     # Request rate, latency, JVM, CPU panels
├── Dockerfile                     # Multi-stage build, non-root user, healthcheck
└── README.md                      # You are here
```

> **Note on the app code**: the upstream `taskmaster` repo ships as a skeleton
> (build files only, no source). The `src/` folder here adds a minimal but
> real Task Management REST API (`/api/tasks` CRUD) with Spring Data JPA,
> plus Actuator (`/actuator/health`) and Micrometer-Prometheus
> (`/actuator/prometheus`) wired in — these two endpoints are what the ALB
> health check and the Prometheus scrape config depend on.

## Setup & Deployment

### Prerequisites
- AWS CLI configured with credentials that can create VPC/ECS/RDS/IAM resources
- Terraform >= 1.5.0
- Docker (for local testing)

### 1. Provision infrastructure
```bash
cd terraform
terraform init
terraform plan
terraform apply
```
This creates the VPC, ECR repo, RDS database, ECS cluster (with an initial
task pointing at a placeholder image), and the ALB.

### 3. Configure GitHub Actions secrets
In the `taskmaster` repo's GitHub settings, add:
| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM user with ECR push + ECS deploy permissions |
| `AWS_SECRET_ACCESS_KEY` | corresponding secret key |
| `ALB_DNS_NAME` | output of `terraform output alb_dns_name` |

### 4. Push to `main`
The GitHub Actions pipeline (`.github/workflows/deploy.yml`) will:
1. Run unit/integration tests
2. Build the Docker image and push to ECR
3. Deploy the new image to the ECS service (zero-downtime rolling update)
4. Run a smoke test against `/actuator/health` through the ALB

### 5. View monitoring locally
```bash
cd monitoring
docker compose -f docker-compose.monitoring.yml up
```
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (login: `admin` / `admin`)

To point Prometheus at the deployed ALB instead of localhost, edit
`monitoring/prometheus.yml` and swap the target as noted in the file's comments.

## Security Notes
- ECS tasks and the RDS instance are only reachable from within the VPC —
  no public IPs assigned.
- DB credentials are randomly generated by Terraform and stored in Secrets
  Manager, never committed to source control or hardcoded in the task definition.
- ECR image scanning is enabled on push.
- Container runs as a non-root user (see `Dockerfile`).

## Scaling & Maintainability
- `desired_count` and `container_cpu`/`container_memory` are Terraform
  variables — scaling the service up/down is a one-line change + `terraform apply`.
- The ECS service definition ignores task-definition drift
  (`lifecycle.ignore_changes`), so CI/CD deploys don't fight with Terraform
  state on every run.
- ECR lifecycle policy automatically prunes old images beyond the last 10,
  keeping storage costs predictable.

## Cost Note
This stack uses Fargate (pay-per-task), a single NAT Gateway, and a
`db.t3.micro` RDS instance (Free Tier eligible for 12 months on a new AWS
account). Remember to run `terraform destroy` after the evaluation to avoid
ongoing charges — the NAT Gateway in particular bills hourly.
