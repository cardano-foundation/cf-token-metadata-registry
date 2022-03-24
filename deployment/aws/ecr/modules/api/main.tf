locals {
  name_prefix = "${var.project}-${var.environment}"
}

module "api" {
  source = "./api-service"

  project     = var.project
  environment = var.environment
  region      = var.region
  name_prefix = local.name_prefix
}

module "gitsync" {
  source = "./gitsync-service"

  project     = var.project
  environment = var.environment
  region      = var.region
  name_prefix = local.name_prefix
}
