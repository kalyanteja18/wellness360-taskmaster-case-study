output "alb_dns_name" {
  description = "Public URL to access the deployed application"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "ECR repo URL — used by GitHub Actions to push images"
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  value = aws_ecs_service.app.name
}

output "vpc_id" {
  value = aws_vpc.main.id
}
