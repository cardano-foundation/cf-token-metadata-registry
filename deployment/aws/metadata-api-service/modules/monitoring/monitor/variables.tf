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

variable "schedule_expression" {
  description = "Defines the frequency for monitors to perform checks."
}

variable "target_base_path" {
  description = "The base path URL of the API server that is monitored."
}

variable "target_subject" {
  description = "The cardano metadata subject that is probed for monitoring."
}

variable "target_connect_timeout" {
  description = "Milliseconds to wait for connection with the API host targeted by the monitor."
  type        = number
}

variable "metric_region" {
  description = "The region the gathered cloudwatch metrics shall be published to."
}

variable "ops_bucket" {
  description = "The S3 bucket used for deployments or other non functional tasks like logging."
}
