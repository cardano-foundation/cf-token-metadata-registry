region                      = "eu-west-1"
environment                 = "dev"
project                     = "cf-metadata-api"
domain_name                 = "metadata.dev.cf-deployments.org"
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
gitsync_schedule_expression = "rate(6 hours)"
loadtest_image_version      = "latest"
loadtest_task_cpu           = 4096
loadtest_task_memory        = 8192
loadtest_region_config      = "euc1:1,use1:1,apse1:1"
lb_access_logs_enabled      = false

service_config = {
  "eu1" = {
    rds_instance_type = "db.t4g.micro",
    cpu               = 512,
    memory            = 1024,
    min_tasks         = 1,
    max_tasks         = 8,
    region            = "eu-west-1"
  },
  "us1" = {
    rds_instance_type = "db.t4g.micro",
    cpu               = 512,
    memory            = 1024,
    min_tasks         = 1,
    max_tasks         = 8,
    region            = "us-east-2"
  }
}

monitoring_config = {
  "eu1" = {
    schedule_expression    = "rate(15 minutes)",
    region                 = "eu-central-1",
    target_base_path       = "https://api.metadata.dev.cf-deployments.org",
    target_subject         = "9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145",
    target_connect_timeout = 5000
  },
  "us1" = {
    schedule_expression    = "rate(15 minutes)",
    region                 = "us-east-1",
    target_base_path       = "https://api.metadata.dev.cf-deployments.org",
    target_subject         = "9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145",
    target_connect_timeout = 5000
  },
  "ap1" = {
    schedule_expression    = "rate(15 minutes)",
    region                 = "ap-southeast-1",
    target_base_path       = "https://api.metadata.dev.cf-deployments.org",
    target_subject         = "9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145",
    target_connect_timeout = 5000
  },
}
