locals {
  name_prefix = "${var.name_prefix}-monitoring"
}

module "monitor-eu1" {
  source = "./monitor"

  providers = {
    aws = aws.eu1
  }

  project                = var.project
  environment            = var.environment
  region                 = var.config["eu1"].region
  name_prefix            = local.name_prefix
}

module "monitor-us1" {
  source = "./monitor"

  providers = {
    aws = aws.us1
  }

  project                = var.project
  environment            = var.environment
  region                 = var.config["us1"].region
  name_prefix            = local.name_prefix
}

module "monitor-ap1" {
  source = "./monitor"

  providers = {
    aws = aws.ap1
  }

  project                = var.project
  environment            = var.environment
  region                 = var.config["ap1"].region
  name_prefix            = local.name_prefix
}
