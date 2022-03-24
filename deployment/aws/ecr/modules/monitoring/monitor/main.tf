locals {
  name_prefix = "${var.name_prefix}-${var.region}"
}

resource "aws_kms_key" "ecr_key" {
  description = "This key is used to encrypt the ECR repository of the monitoring lambda images."

  tags = {
    Name = "${local.name_prefix}-ecr-key"
  }
}

resource "aws_kms_alias" "ecr_key_alias" {
  name          = "alias/${local.name_prefix}-ecr-key"
  target_key_id = aws_kms_key.ecr_key.key_id
}

resource "aws_ecr_repository" "ecr_repo" {
  name                 = "${local.name_prefix}-image-repo"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = aws_kms_key.ecr_key.arn
  }
}
