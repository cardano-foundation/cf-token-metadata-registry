locals {
  name_prefix = "${var.project}-${var.environment}"
}

module "api-eu1" {
  source = "./modules/api"

  providers = {
    aws = aws
  }

  project     = var.project
  environment = var.environment
  region      = var.service_config["eu1"].region
}

module "api-us1" {
  source = "./modules/api"

  providers = {
    aws = aws.us1
  }

  project     = var.project
  environment = var.environment
  region      = var.service_config["us1"].region
}

module "api-ap1" {
  count  = var.environment == "prod" ? 1 : 0
  source = "./modules/api"

  providers = {
    aws = aws.ap1
  }

  project     = var.project
  environment = var.environment
  region      = var.service_config["ap1"].region
}

module "loadtest" {
  source = "./modules/loadtest"

  project                = var.project
  environment            = var.environment
  region                 = var.region
  name_prefix            = local.name_prefix
}

module "monitoring" {
  source = "./modules/monitoring"

  providers = {
    aws.eu1 = aws.monitor_eu1
    aws.us1 = aws.monitor_us1
    aws.ap1 = aws.monitor_ap1
  }

  project     = var.project
  environment = var.environment
  config      = var.monitoring_config
  name_prefix = local.name_prefix
}
