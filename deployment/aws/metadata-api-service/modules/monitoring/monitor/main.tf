locals {
  name_prefix = "${var.name_prefix}-${var.region}"
}

resource "aws_kms_key" "monitoring_kms_key" {
  description = "This key is used to encrypt the S3 bucket used for lambda deployments."

  tags = {
    Name = "${local.name_prefix}-kms-key"
  }
}

resource "aws_kms_alias" "monitoring_kms_key_alias" {
  name          = "alias/${local.name_prefix}-key"
  target_key_id = aws_kms_key.monitoring_kms_key.key_id
}

resource "aws_ecr_repository" "lambda_ecr_repo" {
  name                 = "${local.name_prefix}-image-repo"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = aws_kms_key.monitoring_kms_key.arn
  }
}

resource "aws_lambda_function" "monitor" {
  function_name = "ApiMonitor-${var.environment}"

  image_uri    = "${aws_ecr_repository.lambda_ecr_repo.repository_url}:latest"
  memory_size  = 128
  timeout      = 45
  package_type = "Image"

  environment {
    variables = {
      REGION                 = var.region
      BASE_PATH              = var.target_base_path
      PROBING_SUBJECT        = var.target_subject
      CONNECT_TIMEOUT        = var.target_connect_timeout
      CLOUDWATCH_REGION      = var.metric_region
      DEPLOYMENT_ENVIRONMENT = var.environment
    }
  }

  role = aws_iam_role.monitor_execution_role.arn
}

resource "aws_iam_role" "monitor_execution_role" {
  name                = "${local.name_prefix}-lambda_role"
  managed_policy_arns = [aws_iam_policy.monitor_policy_custom.arn, "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"]

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

resource "aws_iam_policy" "monitor_policy_custom" {
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "cloudwatch:PutMetricData"
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
      }
    ]
  })
}

resource "aws_cloudwatch_event_rule" "monitor_event_rule" {
  name                = "${local.name_prefix}-monitoring-trigger"
  description         = "Triggers the monitoring lambda."
  schedule_expression = var.schedule_expression
}

resource "aws_cloudwatch_event_target" "monitor_event_target" {
  rule      = aws_cloudwatch_event_rule.monitor_event_rule.name
  target_id = "${local.name_prefix}-monitor-target"
  arn       = aws_lambda_function.monitor.arn
}

resource "aws_lambda_permission" "monitor_trigger_premission" {
  statement_id  = "${local.name_prefix}-AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.monitor.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.monitor_event_rule.arn
}
