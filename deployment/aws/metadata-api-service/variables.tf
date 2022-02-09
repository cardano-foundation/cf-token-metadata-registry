variable "region" {
  description = "AWS region"
}

variable "aws-profile-name" {
  description = "The AWS credential profile to use."
}

variable "aws-account-id" {
  description = "The AWS account id to use."
}

variable "project" {
  description = "The project name."
}

variable "environment" {
  description = "The environent/stage name for the deployment (e.g. dev, int, prod, qa, etc.)"
}

variable "vpc_cidr" {
  description = "The CIDR block of the vpc"
}

variable "service_image_version" {
  description = "The version tag of the container image of the backend service."
}

variable "service_task_container_port" {
  description = "The port that is exposed by the container running the backend API within an ECS task."
}

variable "rds_instance_name" {
  description = "The RDS instance name"
}

variable "rds_admin_user_name" {
  description = "The name of the master user"
}

variable "rds_port" {
  description = "The port that shall be exposed by the RDS instance"
  type        = number
  default     = 5432
}

variable "rds_multi_az" {
  description = "The DB instance name"
  type        = bool
  default     = false
}

variable "rds_min_storage" {
  description = "Minimum amount of storage assigned to the instance in GB"
  type        = number
}

variable "rds_max_storage" {
  description = "Minimum amount of storage assigned to the instance in GB"
  type        = number
}

variable "service_user_name" {
  description = "Username part of the credentials for the service user."
}

variable "metadata_db_name" {
  description = "Database name of the database that stores the token metadata."
}

variable "database_driver_class_name" {
  description = "Name of the driver class to use by the backend for RDS connectivity."
}

variable "gitsync_image_version" {
  description = "The version tag of the container image of the gitsync task."
}

variable "gitsync_task_cpu" {
  description = "AWS Fargate CPU units configuration for a single ECS task of the gitsync task."
}

variable "gitsync_task_memory" {
  description = "AWS Fargate memory units configuration for a single ECS task of the gitsync task."
}

variable "gitsync_schedule_expression" {
  description = "The schedule expression used to schedule the execution of the gitsync ECS task."
}

variable "domain_name" {
  description = "Domain name where the API is hosted."
}

variable "loadtest_image_version" {
  description = "The version tag of the container image of the loadtest task."
}

variable "loadtest_task_cpu" {
  description = "AWS Fargate CPU units configuration for a single ECS task of the loadtest task."
}

variable "loadtest_task_memory" {
  description = "AWS Fargate memory units configuration for a single ECS task of the loadtest task."
}

variable "loadtest_region_config" {
  description = "The configuration for the number of loadtest tasks started in each region."
  type        = string
  default     = "euc1:1,use1:1,apse1:1"
}

variable "service_config" {
  description = "Configuration for the ECS services and RDS instances hosted in different regions."
  type = map(object({
    rds_instance_type = string,
    cpu               = number,
    memory            = number,
    min_tasks         = number,
    max_tasks         = number,
    region            = string
  }))
}

variable "monitoring_config" {
  description = "Configuration for the montoring lambda instances."
  type = map(object({
    region                 = string,
    schedule_expression    = string,
    target_base_path       = string,
    target_subject         = string,
    target_connect_timeout = number
  }))
}
