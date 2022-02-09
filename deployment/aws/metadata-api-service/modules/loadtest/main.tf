locals {
  name_prefix = "${var.name_prefix}-loadtest"
}

resource "aws_s3_bucket" "loadtest_bucket" {
  bucket = "${local.name_prefix}-data"
  acl    = "private"

  versioning {
    enabled = false
  }

  logging {
    target_bucket = var.log_bucket.id
    target_prefix = "log/"
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = var.default_ops_key.arn
        sse_algorithm     = "aws:kms"
      }
    }
  }
}

resource "aws_s3_bucket_public_access_block" "loadtest_bucket_blocking" {
  bucket = aws_s3_bucket.loadtest_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_kms_key" "loadtest_ecr_key" {
  description = "This key is used to encrypt the ecr repository for loadtesting container images."

  tags = {
    Name = "${local.name_prefix}-ecr-key"
  }
}

resource "aws_kms_alias" "loadtest_ecr_key_alias" {
  name          = "alias/${local.name_prefix}-ecr-key"
  target_key_id = aws_kms_key.loadtest_ecr_key.key_id
}

resource "aws_ecr_repository" "loadtest_ecr_repo" {
  name                 = "${local.name_prefix}-image-repo"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = aws_kms_key.loadtest_ecr_key.arn
  }
}

resource "aws_iam_role" "ecs_execution_role" {
  name                = "${local.name_prefix}-ecs-exec-role"
  path                = "/"
  managed_policy_arns = ["arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"]
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        },
        Effect = "Allow",
        Sid    = ""
      }
    ]
  })
}

resource "aws_iam_role" "ecs_task_role" {
  name                = "${local.name_prefix}-ecs-task-role"
  path                = "/"
  managed_policy_arns = [aws_iam_policy.ecs_task_role_policy.arn]
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        },
        Effect = "Allow",
        Sid    = ""
      }
    ]
  })
}

resource "aws_iam_policy" "ecs_task_role_policy" {
  name        = "${local.name_prefix}-ecs-task-policy"
  description = "Policy for the loadtest ECS tasks."
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "ssm:GetParameters",
          "ssm:GetParameter"
        ]
        Effect   = "Allow"
        Resource = "*"
      },
      {
        Action = [
          "kms:Decrypt"
        ]
        Effect   = "Allow"
        Resource = "*"
      },
      {
        Action = [
          "s3:ListBucket"
        ]
        Effect   = "Allow"
        Resource = "${aws_s3_bucket.loadtest_bucket.arn}"
      },
      {
        Action = [
          "s3:GetObject",
          "s3:GetObjectVersion",
          "s3:PutObject",
          "s3:PutObjectAcl"
        ]
        Effect   = "Allow"
        Resource = "${aws_s3_bucket.loadtest_bucket.arn}/*"
      },
      {
        "Action" = [
          "kms:Decrypt",
          "kms:GenerateDataKey"
        ],
        "Effect"   = "Allow",
        "Resource" = var.default_ops_key.arn
      }
    ]
  })
}

module "loadtest-euc1" {
  source = "./loadtest-task"

  providers = {
    aws = aws.eu1
  }

  project              = var.project
  environment          = var.environment
  region               = "eu-central-1"
  name_prefix          = local.name_prefix
  task_cpu             = var.task_cpu
  task_memory          = var.task_memory
  ecr_image_version    = var.ecr_image_version
  loadtest_bucket      = aws_s3_bucket.loadtest_bucket
  loadtest_kms_key_arn = var.default_ops_key.arn
  ecr_repository_url   = aws_ecr_repository.loadtest_ecr_repo.repository_url
  execution_role_arn   = aws_iam_role.ecs_execution_role.arn
  task_role_arn        = aws_iam_role.ecs_task_role.arn
}


module "loadtest-use1" {
  source = "./loadtest-task"

  providers = {
    aws = aws.us1
  }

  project              = var.project
  environment          = var.environment
  region               = "us-east-1"
  name_prefix          = local.name_prefix
  task_cpu             = var.task_cpu
  task_memory          = var.task_memory
  ecr_image_version    = var.ecr_image_version
  loadtest_bucket      = aws_s3_bucket.loadtest_bucket
  loadtest_kms_key_arn = var.default_ops_key.arn
  ecr_repository_url   = aws_ecr_repository.loadtest_ecr_repo.repository_url
  execution_role_arn   = aws_iam_role.ecs_execution_role.arn
  task_role_arn        = aws_iam_role.ecs_task_role.arn
}

module "loadtest-apse1" {
  source = "./loadtest-task"

  providers = {
    aws = aws.ap1
  }

  project              = var.project
  environment          = var.environment
  region               = "ap-southeast-1"
  name_prefix          = local.name_prefix
  task_cpu             = var.task_cpu
  task_memory          = var.task_memory
  ecr_image_version    = var.ecr_image_version
  loadtest_bucket      = aws_s3_bucket.loadtest_bucket
  loadtest_kms_key_arn = var.default_ops_key.arn
  ecr_repository_url   = aws_ecr_repository.loadtest_ecr_repo.repository_url
  execution_role_arn   = aws_iam_role.ecs_execution_role.arn
  task_role_arn        = aws_iam_role.ecs_task_role.arn
}

# TEST RUNNER LAMBDA
data "archive_file" "lambda_test_runner_archive" {
  type = "zip"

  source_dir  = "${path.module}/test-runner"
  output_path = "${path.module}/.build/test-runner.zip"
}

resource "aws_s3_bucket_object" "lambda_test_runner_deployment_package" {
  bucket = aws_s3_bucket.loadtest_bucket.id

  key    = "lambda-deployments/test-runner/test-runner.zip"
  source = data.archive_file.lambda_test_runner_archive.output_path

  etag = filemd5(data.archive_file.lambda_test_runner_archive.output_path)
}

resource "aws_lambda_function" "loadtest_runner" {
  function_name = "LoadtestRunner-${var.environment}"

  s3_bucket   = aws_s3_bucket.loadtest_bucket.id
  s3_key      = aws_s3_bucket_object.lambda_test_runner_deployment_package.id
  runtime     = "python3.9"
  handler     = "run_loadtest.handler"
  memory_size = 128
  timeout     = 300

  environment {
    variables = {
      LOADTEST_REGION_CONFIG        = var.loadtest_region_config
      REGION_euc1                   = module.loadtest-euc1.region
      ECS_TASK_DEFINITION_ARN_euc1  = module.loadtest-euc1.ecs_task_definition_arn
      CLUSTER_ARN_euc1              = module.loadtest-euc1.ecs_cluster_arn
      SECURITY_GROUP_ID_euc1        = module.loadtest-euc1.loadtest_security_group_id
      SUBNET_ID_euc1                = module.loadtest-euc1.target_subnet_ids[0]
      REGION_use1                   = module.loadtest-use1.region
      ECS_TASK_DEFINITION_ARN_use1  = module.loadtest-use1.ecs_task_definition_arn
      CLUSTER_ARN_use1              = module.loadtest-use1.ecs_cluster_arn
      SECURITY_GROUP_ID_use1        = module.loadtest-use1.loadtest_security_group_id
      SUBNET_ID_use1                = module.loadtest-use1.target_subnet_ids[0]
      REGION_apse1                  = module.loadtest-apse1.region
      ECS_TASK_DEFINITION_ARN_apse1 = module.loadtest-apse1.ecs_task_definition_arn
      CLUSTER_ARN_apse1             = module.loadtest-apse1.ecs_cluster_arn
      SECURITY_GROUP_ID_apse1       = module.loadtest-apse1.loadtest_security_group_id
      SUBNET_ID_apse1               = module.loadtest-apse1.target_subnet_ids[0]
    }
  }

  source_code_hash = data.archive_file.lambda_test_runner_archive.output_base64sha256

  role = aws_iam_role.test_runner_execution_role.arn
}

resource "aws_iam_role" "test_runner_execution_role" {
  name                = "${local.name_prefix}-runner_lambda_role"
  managed_policy_arns = [aws_iam_policy.test_runner_policy_custom.arn]

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Sid    = ""
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "test_runner_policy_basic_aws" {
  role       = aws_iam_role.test_runner_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_policy" "test_runner_policy_custom" {
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "ecs:RunTask"
        ]
        Effect = "Allow"
        Resource = [
          "${module.loadtest-euc1.ecs_task_definition_arn}",
          "${module.loadtest-use1.ecs_task_definition_arn}",
          "${module.loadtest-apse1.ecs_task_definition_arn}"
        ]
      },
      {
        Action = [
          "ecs:DescribeTasks",
          "ecs:DescribeTaskDefinition"
        ]
        Effect   = "Allow"
        Resource = "*"
      },
      {
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Effect     = "Allow"
        "Resource" = "arn:aws:logs:*:*:*"
      },
      {
        Action = [
          "iam:PassRole"
        ]
        Effect = "Allow"
        "Resource" = [
          "${aws_iam_role.ecs_task_role.arn}",
          "${aws_iam_role.ecs_execution_role.arn}"
        ]
      }
    ]
  })
}

# RESULTS PROCESSOR LAMBDA
data "archive_file" "lambda_results_processor_archive" {
  type = "zip"

  source_dir  = "${path.module}/results-processor"
  output_path = "${path.module}/.build/results-processor.zip"
}

resource "aws_s3_bucket_object" "lambda_results_processor_deployment_package" {
  bucket = aws_s3_bucket.loadtest_bucket.id

  key    = "/lambda-deployments/results-processor/results-processor.zip"
  source = data.archive_file.lambda_results_processor_archive.output_path

  etag = filemd5(data.archive_file.lambda_results_processor_archive.output_path)
}
