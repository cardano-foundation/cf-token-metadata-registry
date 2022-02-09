output "rds_instance_id" {
  value = aws_db_instance.metadata_db.id
}

output "rds_instance_name" {
  value = aws_db_instance.metadata_db.name
}

output "rds_instance_endpoint" {
  value = aws_db_instance.metadata_db.endpoint
}

output "rds_instance_port" {
  value = aws_db_instance.metadata_db.port
}

output "bastion_instance_id" {
  value = aws_instance.bastion.id
}

output "rds_secret_arn" {
  value = aws_secretsmanager_secret_version.master_user_secret_version.arn
}

output "access_to_rds_security_group_id" {
  value = aws_security_group.access_to_rds_sg.id
}
