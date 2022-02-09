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

variable "loadtest_bucket" {
  description = "The s3 bucket where loadtest results get stored to."
}

variable "loadtest_kms_key_arn" {
  description = "ARN of the kms key used for encryption of data in the loadtest_bucket."
}

variable "ecr_repository_url" {
  description = "The URL of the ECR repository holding the container images used for loadtesting."
}

variable "execution_role_arn" {
  description = "The ARN of the IAM execution role for the loadtest ECS tasks."
}

variable "task_role_arn" {
  description = "The ARN of the IAM role for the loadtest ECS tasks."
}
