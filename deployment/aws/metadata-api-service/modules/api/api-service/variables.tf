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

variable "database_driver_class_name" {
  description = "Name of the driver class to use by the backend for RDS connectivity."
}

variable "service_user_name" {
  description = "Username part of the credentials for the service user."
}

variable "metadata_db_name" {
  description = "Database name of the database that stores the token metadata."
}

variable "service_image_version" {
  description = "The version tag of the container image of the backend service."
}

variable "service_task_cpu" {
  description = "AWS Fargate CPU units configuration for a single ECS task."
}

variable "service_task_memory" {
  description = "AWS Fargate memory units configuration for a single ECS task."
}

variable "service_task_count_min" {
  description = "Minimum number of running ECS tasks within the backend API ECS service."
}

variable "service_task_count_max" {
  description = "Maximum number of running ECS tasks within the backend API ECS service."
}

variable "service_task_container_port" {
  description = "The port that is exposed by the container running the backend API within an ECS task."
}

variable "ecs_target_subnets" {
  description = "Target subnets for the ECS tasks of the service."
  type = list
}

variable "ecs_target_security_groups" {
  description = "Additional security groups for the ECS tasks of the service"
  type = list
}

variable "ecs_target_assign_public_ip" {
  description = "Specify if the tasks shall get a public ip or not."
  type = bool
  default = false
}

variable "lb_target_subnets" {
  description = "The target subnets of the public loadbalancer."
  type = list
}

variable "vpc_id" {
  description = "The VPC id the services shall be executed in."
}

variable "vpc_cidr" {
  description = "The CIDR block of the vpc"
}

variable "rds_instance_endpoint" {
  description = "The endpoint URL of the RDS instance to use."
}

variable "domain_name" {
  description = "Domain name where the API is hosted."
}
