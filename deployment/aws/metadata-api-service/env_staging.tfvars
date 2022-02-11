region                      = "eu-west-1"
environment                 = "qa"
project                     = "cf-metadata-api"
domain_name                 = "metadata.staging.cf-deployments.org"
vpc_cidr                    = "10.15.0.0/16"
aws-profile-name            = "AdministratorAccess"
aws-account-id              = "747042292633"
rds_instance_name           = "metadata-cf"
rds_port                    = 5432
rds_admin_user_name         = "cf_admin"
rds_multi_az                = false
rds_min_storage             = 20
rds_max_storage             = 50
service_image_version       = "latest"
service_task_container_port = 8080
service_user_name           = "cf_service"
metadata_db_name            = "cf_metadata"
database_driver_class_name  = "org.postgresql.Driver"
gitsync_image_version       = "latest"
gitsync_task_cpu            = 2048
gitsync_task_memory         = 8192
gitsync_schedule_expression = "rate(1 hour)"
loadtest_image_version      = "latest"
loadtest_task_cpu           = 4096
loadtest_task_memory        = 8192
loadtest_region_config      = "euc1:4,use1:2,apse1:1"
lb_access_logs_enabled      = true

service_config = {
  "eu1" = {
    rds_instance_type = "db.t4g.medium",
    cpu               = 2048,
    memory            = 4096,
    min_tasks         = 2,
    max_tasks         = 64,
    region            = "eu-west-1"
  },
  "us1" = {
    rds_instance_type = "db.t4g.medium",
    cpu               = 1024,
    memory            = 4096,
    min_tasks         = 2,
    max_tasks         = 32,
    region            = "us-east-2"
  },
  "ap1" = {
    rds_instance_type = "db.t4g.small",
    cpu               = 512,
    memory            = 1024,
    min_tasks         = 2,
    max_tasks         = 16,
    region            = "ap-south-1"
  }
}

monitoring_config = {
  "eu1" = {
    schedule_expression    = "rate(1 minute)",
    region                 = "eu-central-1",
    target_base_path       = "https://api.metadata.staging.cf-deployments.org",
    target_subject         = "9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145",
    target_connect_timeout = 5000
  },
  "us1" = {
    schedule_expression    = "rate(1 minute)",
    region                 = "us-east-1",
    target_base_path       = "https://api.metadata.staging.cf-deployments.org",
    target_subject         = "9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145",
    target_connect_timeout = 5000
  },
  "ap1" = {
    schedule_expression    = "rate(1 minute)",
    region                 = "ap-southeast-1",
    target_base_path       = "https://api.metadata.staging.cf-deployments.org",
    target_subject         = "9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145",
    target_connect_timeout = 5000
  },
}
