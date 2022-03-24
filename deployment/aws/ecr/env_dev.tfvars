region           = "eu-west-1"
environment      = "dev"
project          = "cf-metadata-api-ecr"
aws-profile-name = "AdministratorAccess"
aws-account-id   = "747042292633"

service_config = {
  "eu1" = {
    region = "eu-west-1"
  },
  "us1" = {
    region = "us-east-2"
  }
}

monitoring_config = {
  "eu1" = {
    region = "eu-central-1"
  },
  "us1" = {
    region = "us-east-1"
  },
  "ap1" = {
    region = "ap-southeast-1"
  },
}
