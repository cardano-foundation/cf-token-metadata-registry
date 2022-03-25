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
  type        = number
}

variable "task_memory" {
  description = "AWS Fargate memory units configuration for a single ECS task of the scheduled task."
  type        = number
}

variable "ecr_image_version" {
  description = "The version tag of the container imageto use by the scheduled task."
}

variable "loadtest_region_config" {
  description = "The configuration for the number of loadtest tasks started in each region."
  type        = string
}

variable "default_ops_key" {
  description = "The default KMS key used for non functional tasks like loadtests."
}

variable "ops_bucket" {
  description = "The bucket used for logging of S3 events."
}