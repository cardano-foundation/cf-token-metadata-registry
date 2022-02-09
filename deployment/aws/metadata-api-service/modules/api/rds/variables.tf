variable "name_prefix" {
  description = "The prefix prepended to each named resource to distinguish between environments and products."
}

variable "region" {
  description = "AWS region"
}

variable "environment" {
  description = "The environent/stage name for the deployment (e.g. dev, int, prod, qa, etc.)"
}

variable "vpc_id" {
  description = "The id of the vpc the RDS instance shall reside in"
}

variable "db_subnet_ids" {
  description = "The ids of the subnets the RDS instance shall reside in"
  type        = list
}

variable "app_subnet_ids" {
  description = "The ids of the subnets the RDS instance shall reside in"
  type        = list
}

variable "instance_name" {
  description = "The RDS instance name"
}

variable "admin_user_name" {
  description = "The name of the master user"
}

variable "port" {
  description = "The port that shall be exposed by the RDS instance"
  type        = number
  default     = 5432
}

variable "instance_type" {
  description = "The RDS instance type"
}

variable "multi_az" {
  description = "The DB instance name"
  type        = bool
  default     = false
}

variable "min_storage" {
  description = "Minimum amount of storage assigned to the instance in GB"
  type        = number
}

variable "max_storage" {
  description = "Minimum amount of storage assigned to the instance in GB"
  type        = number
}

variable "publicly_accessible" {
  description = "Make instance publicly accessible or not"
  type        = bool
  default     = false
}
