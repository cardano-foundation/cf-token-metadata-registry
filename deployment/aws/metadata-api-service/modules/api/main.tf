locals {
  name_prefix = "${var.project}-${var.environment}"
}

# fetch vpc information
data "aws_vpc" "baseline_vpc" {
  tags = {
    Name = "cf-baseline-vpc"
  }
}

data "aws_security_group" "baseline_vpc_default_sg" {
  name = "cf-baseline-vpc-default-sg"
}

data "aws_subnets" "baseline_vpc_pub_subnets" {
  tags = {
    Name = "cf-baseline-vpc-pub-*"
  }
}

data "aws_subnets" "baseline_vpc_app_subnets" {
  tags = {
    Name = "cf-baseline-vpc-app-*"
  }
}

data "aws_subnets" "baseline_vpc_db_subnets" {
  tags = {
    Name = "cf-baseline-vpc-db-*"
  }
}

module "rds" {
  source = "./rds"

  name_prefix         = local.name_prefix
  region              = var.region
  environment         = var.environment
  vpc_id              = data.aws_vpc.baseline_vpc.id
  db_subnet_ids       = data.aws_subnets.baseline_vpc_db_subnets.ids
  app_subnet_ids      = data.aws_subnets.baseline_vpc_app_subnets.ids
  instance_name       = var.rds_instance_name
  port                = var.rds_port
  admin_user_name     = var.rds_admin_user_name
  multi_az            = var.rds_multi_az
  instance_type       = var.rds_instance_type
  min_storage         = var.rds_min_storage
  max_storage         = var.rds_max_storage
  publicly_accessible = false
}

module "api" {
  source = "./api-service"

  project                     = var.project
  environment                 = var.environment
  region                      = var.region
  name_prefix                 = local.name_prefix
  database_driver_class_name  = var.database_driver_class_name
  service_image_version       = var.service_image_version
  service_task_cpu            = var.service_task_cpu
  service_task_memory         = var.service_task_memory
  service_task_count_min      = var.service_task_count_min
  service_task_count_max      = var.service_task_count_max
  service_task_container_port = var.service_task_container_port
  service_user_name           = var.service_user_name
  metadata_db_name            = var.metadata_db_name
  ecs_target_subnets          = data.aws_subnets.baseline_vpc_app_subnets.ids
  ecs_target_security_groups  = [module.rds.access_to_rds_security_group_id]
  ecs_target_assign_public_ip = false
  lb_target_subnets           = data.aws_subnets.baseline_vpc_pub_subnets.ids
  vpc_id                      = data.aws_vpc.baseline_vpc.id
  vpc_cidr                    = var.vpc_cidr
  domain_name                 = var.domain_name
  rds_instance_endpoint       = module.rds.rds_instance_endpoint
  lb_access_logs_enabled      = var.lb_access_logs_enabled
}

module "gitsync" {
  source = "./gitsync-service"

  project                         = var.project
  environment                     = var.environment
  region                          = var.region
  name_prefix                     = local.name_prefix
  task_cpu                        = var.gitsync_task_cpu
  task_memory                     = var.gitsync_task_memory
  ecr_image_version               = var.gitsync_image_version
  event_rule_schedule_expression  = var.gitsync_schedule_expression
  event_rule_description          = "Runs the git-DB sync task on a fixed schedule."
  ecs_target_subnets              = data.aws_subnets.baseline_vpc_app_subnets.ids
  ecs_target_security_groups      = [module.rds.access_to_rds_security_group_id]
  ecs_target_assign_public_ip     = false
  vpc_id                          = data.aws_vpc.baseline_vpc.id
  rds_url_ssm_parameter_name      = module.api.rds_url_ssm_parameter_name
  rds_username_ssm_parameter_name = module.api.rds_username_ssm_parameter_name
  rds_password_ssm_parameter_name = module.api.rds_password_ssm_parameter_name
}
