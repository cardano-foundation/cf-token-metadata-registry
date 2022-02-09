output "service_ecr_repository_url" {
  value = aws_ecr_repository.service_ecr_repo.repository_url
}

output "service_ecr_arn" {
  value = aws_ecr_repository.service_ecr_repo.arn
}

output "rds_url_ssm_parameter_name" {
  value = aws_ssm_parameter.rds_url.name
}

output "rds_username_ssm_parameter_name" {
  value = aws_ssm_parameter.rds_username.name
}

output "rds_password_ssm_parameter_name" {
  value = aws_ssm_parameter.rds_password.name
}
