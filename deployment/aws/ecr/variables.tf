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
  description = "The environent/stage name for the deployment (e.g. dev, qa, prod etc.)"
}

variable "service_config" {
  description = "Configuration for the ECS services and RDS instances hosted in different regions."
  type = map(object({
    region = string
  }))
}

variable "monitoring_config" {
  description = "Configuration for the montoring lambda instances."
  type = map(object({
    region = string
  }))
}
