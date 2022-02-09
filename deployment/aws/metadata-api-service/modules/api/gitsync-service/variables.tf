variable "environment" {
  description = "The environent/stage name for the deployment (e.g. dev, int, prod, qa, etc.)"
}

variable "project" {
  description = "The project name."
}

variable "region" {
  description = "AWS region"
}

variable "name_prefix" {
  description = "The prefix prepended to each named resource to distinguish between environments and products."
}

variable "task_cpu" {
  description = "AWS Fargate CPU units configuration for a single ECS task of the scheduled task."
  type = number
}

variable "task_memory" {
  description = "AWS Fargate memory units configuration for a single ECS task of the scheduled task."
  type = number
}

variable "ecr_image_version" {
  description = "The version tag of the container imageto use by the scheduled task."
}

variable "event_rule_schedule_expression" {
  description = "The url of the container image registry."
}

variable "event_rule_description" {
  description = "Description added to the created event rule resource."
}

variable "ecs_target_subnets" {
  description = "The arn of the ECS cluster to use for task execution."
  type = list
}

variable "ecs_target_security_groups" {
  description = "The arn of the ECS cluster to use for task execution."
  type = list
}

variable "ecs_target_assign_public_ip" {
  description = "The arn of the ECS cluster to use for task execution."
  type = bool
  default = false
}

variable "vpc_id" {
  description = "The VPC id the services shall be executed in."
}

variable "rds_url_ssm_parameter_name" {
  description = "The VPC id the services shall be executed in."
}

variable "rds_username_ssm_parameter_name" {
  description = "The VPC id the services shall be executed in."
}

variable "rds_password_ssm_parameter_name" {
  description = "The VPC id the services shall be executed in."
}
