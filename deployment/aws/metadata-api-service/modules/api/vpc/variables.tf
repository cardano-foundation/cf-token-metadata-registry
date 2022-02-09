variable "environment" {
  description = "The environent/stage name for the deployment (e.g. dev, int, prod, qa, etc.)"
}

variable "vpc_cidr" {
  description = "The CIDR block of the vpc"
}
