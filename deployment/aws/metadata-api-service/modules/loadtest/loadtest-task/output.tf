output "ecs_task_definition_arn" {
  value = aws_ecs_task_definition.task_definition.arn
}

output "ecs_cluster_arn" {
  value = aws_ecs_cluster.cluster.arn
}

output "loadtest_security_group_id" {
  value = aws_security_group.task_sg.id
}

output "target_subnet_ids" {
  value = data.aws_subnets.default.ids
}

output "region" {
  value = "${var.region}"
}