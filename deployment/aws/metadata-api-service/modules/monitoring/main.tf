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
  schedule_expression    = var.config["eu1"].schedule_expression
  target_base_path       = var.config["eu1"].target_base_path
  target_subject         = var.config["eu1"].target_subject
  target_connect_timeout = var.config["eu1"].target_connect_timeout
  metric_region          = var.metric_region
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
  schedule_expression    = var.config["us1"].schedule_expression
  target_base_path       = var.config["us1"].target_base_path
  target_subject         = var.config["us1"].target_subject
  target_connect_timeout = var.config["us1"].target_connect_timeout
  metric_region          = var.metric_region
}

module "monitor-ap1" {
  count  = var.environment == "prod" ? 1 : 0
  source = "./monitor"

  providers = {
    aws = aws.ap1
  }

  project                = var.project
  environment            = var.environment
  region                 = var.config["ap1"].region
  name_prefix            = local.name_prefix
  schedule_expression    = var.config["ap1"].schedule_expression
  target_base_path       = var.config["ap1"].target_base_path
  target_subject         = var.config["ap1"].target_subject
  target_connect_timeout = var.config["ap1"].target_connect_timeout
  metric_region          = var.metric_region
}

