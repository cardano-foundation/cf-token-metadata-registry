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

variable "config" {
  description = "Configuration for the montoring lambda instances."
  type = map(object({
    region                 = string,
    schedule_expression    = string,
    target_base_path       = string,
    target_subject         = string,
    target_connect_timeout = number
  }))
}

variable "metric_region" {
  description = "The region the gathered cloudwatch metrics shall be published to."
}
