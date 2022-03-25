locals {
  name_prefix = "${var.project}-${var.environment}"
}

data "aws_vpc" "baseline_vpc" {
  tags = {
    Name = "cf-baseline-vpc"
  }
}

resource "aws_kms_key" "default_ops_key" {
  description             = "This key is used to encrypt non-functional bucket objects"
  deletion_window_in_days = 10
}

resource "aws_kms_alias" "default_ops_key_alias" {
  name          = "alias/default_ops_key_${var.environment}"
  target_key_id = aws_kms_key.default_ops_key.key_id
}

resource "aws_s3_bucket" "ops_bucket" {
  bucket = "cf-metadata-${var.environment}-ops"
}

resource "aws_s3_bucket_acl" "ops_bucket_acl" {
  bucket = aws_s3_bucket.ops_bucket.id
  acl    = "private"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "ops_bucket_server_side_encryption_config" {
  bucket = aws_s3_bucket.ops_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.default_ops_key.arn
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "ops_bucket_blocking" {
  bucket = aws_s3_bucket.ops_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

module "api-eu1" {
  source = "./modules/api"

  providers = {
    aws = aws
  }

  project                     = var.project
  environment                 = var.environment
  region                      = var.service_config["eu1"].region
  vpc_cidr                    = data.aws_vpc.baseline_vpc.cidr_block
  rds_instance_name           = var.rds_instance_name
  rds_port                    = var.rds_port
  rds_admin_user_name         = var.rds_admin_user_name
  rds_multi_az                = var.rds_multi_az
  rds_instance_type           = var.service_config["eu1"].rds_instance_type
  rds_min_storage             = var.rds_min_storage
  rds_max_storage             = var.rds_max_storage
  database_driver_class_name  = var.database_driver_class_name
  service_image_version       = var.service_image_version
  service_task_cpu            = var.service_config["eu1"].cpu
  service_task_memory         = var.service_config["eu1"].memory
  service_task_count_min      = var.service_config["eu1"].min_tasks
  service_task_count_max      = var.service_config["eu1"].max_tasks
  service_task_container_port = var.service_task_container_port
  service_user_name           = var.service_user_name
  metadata_db_name            = var.metadata_db_name
  domain_name                 = var.domain_name
  gitsync_task_cpu            = var.gitsync_task_cpu
  gitsync_task_memory         = var.gitsync_task_memory
  gitsync_image_version       = var.gitsync_image_version
  gitsync_schedule_expression = var.gitsync_schedule_expression
  lb_access_logs_enabled      = var.lb_access_logs_enabled
}

module "api-us1" {
  source = "./modules/api"

  providers = {
    aws = aws.us1
  }

  project                     = var.project
  environment                 = var.environment
  region                      = var.service_config["us1"].region
  vpc_cidr                    = data.aws_vpc.baseline_vpc.cidr_block
  rds_instance_name           = var.rds_instance_name
  rds_port                    = var.rds_port
  rds_admin_user_name         = var.rds_admin_user_name
  rds_multi_az                = var.rds_multi_az
  rds_instance_type           = var.service_config["us1"].rds_instance_type
  rds_min_storage             = var.rds_min_storage
  rds_max_storage             = var.rds_max_storage
  database_driver_class_name  = var.database_driver_class_name
  service_image_version       = var.service_image_version
  service_task_cpu            = var.service_config["us1"].cpu
  service_task_memory         = var.service_config["us1"].memory
  service_task_count_min      = var.service_config["us1"].min_tasks
  service_task_count_max      = var.service_config["us1"].max_tasks
  service_task_container_port = var.service_task_container_port
  service_user_name           = var.service_user_name
  metadata_db_name            = var.metadata_db_name
  domain_name                 = var.domain_name
  gitsync_task_cpu            = var.gitsync_task_cpu
  gitsync_task_memory         = var.gitsync_task_memory
  gitsync_image_version       = var.gitsync_image_version
  gitsync_schedule_expression = var.gitsync_schedule_expression
  lb_access_logs_enabled      = var.lb_access_logs_enabled
}

module "api-ap1" {
  count  = var.environment == "prod" ? 1 : 0
  source = "./modules/api"

  providers = {
    aws = aws.ap1
  }

  project                     = var.project
  environment                 = var.environment
  region                      = var.service_config["ap1"].region
  vpc_cidr                    = data.aws_vpc.baseline_vpc.cidr_block
  rds_instance_name           = var.rds_instance_name
  rds_port                    = var.rds_port
  rds_admin_user_name         = var.rds_admin_user_name
  rds_multi_az                = var.rds_multi_az
  rds_instance_type           = var.service_config["ap1"].rds_instance_type
  rds_min_storage             = var.rds_min_storage
  rds_max_storage             = var.rds_max_storage
  database_driver_class_name  = var.database_driver_class_name
  service_image_version       = var.service_image_version
  service_task_cpu            = var.service_config["ap1"].cpu
  service_task_memory         = var.service_config["ap1"].memory
  service_task_count_min      = var.service_config["ap1"].min_tasks
  service_task_count_max      = var.service_config["ap1"].max_tasks
  service_task_container_port = var.service_task_container_port
  service_user_name           = var.service_user_name
  metadata_db_name            = var.metadata_db_name
  domain_name                 = var.domain_name
  gitsync_task_cpu            = var.gitsync_task_cpu
  gitsync_task_memory         = var.gitsync_task_memory
  gitsync_image_version       = var.gitsync_image_version
  gitsync_schedule_expression = var.gitsync_schedule_expression
  lb_access_logs_enabled      = var.lb_access_logs_enabled
}

module "loadtest" {
  source = "./modules/loadtest"

  providers = {
    aws.eu1 = aws.lt_eu1
    aws.us1 = aws.lt_us1
    aws.ap1 = aws.lt_ap1
  }

  project                = var.project
  environment            = var.environment
  region                 = var.region
  name_prefix            = local.name_prefix
  task_cpu               = var.loadtest_task_cpu
  task_memory            = var.loadtest_task_memory
  ecr_image_version      = var.loadtest_image_version
  loadtest_region_config = var.loadtest_region_config
  default_ops_key        = aws_kms_key.default_ops_key
  ops_bucket             = aws_s3_bucket.ops_bucket
}

module "monitoring" {
  source = "./modules/monitoring"

  providers = {
    aws.eu1 = aws.monitor_eu1
    aws.us1 = aws.monitor_us1
    aws.ap1 = aws.monitor_ap1
  }

  project       = var.project
  environment   = var.environment
  region        = var.region
  config        = var.monitoring_config
  name_prefix   = local.name_prefix
  metric_region = var.region
  ops_bucket    = aws_s3_bucket.ops_bucket
}
