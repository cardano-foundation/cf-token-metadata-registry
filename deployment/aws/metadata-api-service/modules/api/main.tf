locals {
  name_prefix = "${var.project}-${var.environment}"
}

module "vpc" {
  source = "./vpc"

  environment = var.environment
  vpc_cidr    = var.vpc_cidr
}

module "rds" {
  source = "./rds"

  name_prefix         = local.name_prefix
  region              = var.region
  environment         = var.environment
  vpc_id              = module.vpc.vpc_id
  db_subnet_ids       = module.vpc.db_subnets_id
  app_subnet_ids      = module.vpc.app_subnets_id
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
  ecs_target_subnets          = module.vpc.app_subnets_id
  ecs_target_security_groups  = [module.rds.access_to_rds_security_group_id]
  ecs_target_assign_public_ip = false
  lb_target_subnets           = module.vpc.pub_subnets_id
  vpc_id                      = module.vpc.vpc_id
  vpc_cidr                    = var.vpc_cidr
  domain_name                 = var.domain_name
  rds_instance_endpoint       = module.rds.rds_instance_endpoint
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
  ecs_target_subnets              = module.vpc.app_subnets_id
  ecs_target_security_groups      = [module.rds.access_to_rds_security_group_id]
  ecs_target_assign_public_ip     = false
  vpc_id                          = module.vpc.vpc_id
  rds_url_ssm_parameter_name      = module.api.rds_url_ssm_parameter_name
  rds_username_ssm_parameter_name = module.api.rds_username_ssm_parameter_name
  rds_password_ssm_parameter_name = module.api.rds_password_ssm_parameter_name
}
