output "rds_url_ssm_parameter_name" {
  value = aws_ssm_parameter.rds_url.name
}

output "rds_username_ssm_parameter_name" {
  value = aws_ssm_parameter.rds_username.name
}

output "rds_password_ssm_parameter_name" {
  value = aws_ssm_parameter.rds_password.name
}
