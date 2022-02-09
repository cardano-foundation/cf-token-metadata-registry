resource "aws_kms_key" "deployment_kms_key" {
  description             = "This key is used to encrypt non-functional bucket objects used for deployments."
  deletion_window_in_days = 10
}

resource "aws_kms_alias" "deployment_kms_key_alias" {
  name          = "alias/deployment_kms_key"
  target_key_id = aws_kms_key.deployment_kms_key.key_id
}

resource "aws_s3_bucket" "terraform_deployment" {
  bucket = "cf-metadata-tf-deployments"
  acl    = "private"

  versioning {
    enabled = true
  }

  lifecycle {
    prevent_destroy = true
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = aws_kms_key.deployment_kms_key.arn
        sse_algorithm     = "aws:kms"
      }
    }
  }
}

resource "aws_s3_bucket_public_access_block" "terraform_deployment_bucket_blocking" {
  bucket = aws_s3_bucket.terraform_deployment.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "terraform_state_lock" {
  name           = "tf-deployment"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  server_side_encryption {
    enabled     = true
    kms_key_arn = aws_kms_key.deployment_kms_key.arn
  }
}
