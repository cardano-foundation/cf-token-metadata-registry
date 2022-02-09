terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.72.0"
      configuration_aliases = [ aws.us1, aws.eu1, aws.ap1 ]
    }

    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.2.0"
    }
  }
}
