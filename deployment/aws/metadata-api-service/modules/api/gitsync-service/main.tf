locals {
  name_prefix = "${var.name_prefix}-gitsync"
  iam_name_prefix = "${var.name_prefix}-${var.region}-gitsync"
}

resource "aws_kms_key" "ecr_key" {
  description = "This key is used to encrypt the ecr repository"

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
    kms_key = aws_kms_key.ecr_key.arn
  }
}

resource "aws_cloudwatch_log_group" "ecs_lg" {
  name = "${local.name_prefix}-ecs-logs"
}

resource "aws_cloudwatch_log_group" "ecs_cluster_lg" {
  name = "${local.name_prefix}-ecs-cluster-logs"
}

resource "aws_ecs_cluster" "gitsync_service_cluster" {
  name = "${local.name_prefix}-cluster"

  configuration {
    execute_command_configuration {
      logging = "OVERRIDE"

      log_configuration {
        cloud_watch_encryption_enabled = false
        cloud_watch_log_group_name     = aws_cloudwatch_log_group.ecs_cluster_lg.name
      }
    }
  }
}

resource "aws_security_group" "gitsync_service_sg" {
  name        = "${local.name_prefix}-sg"
  description = "Security group for ECS tasks running the Git-DB-sync service."
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "-1"
  }
}

resource "aws_iam_role" "ecs_execution_role" {
  name                = "${local.iam_name_prefix}-ecs-exec-role"
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
  name                = "${local.iam_name_prefix}-ecs-task-role"
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
  name        = "${local.iam_name_prefix}-ecs-task-policy"
  description = "Policy for the ecs tasks."
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
      }
    ]
  })
}

resource "aws_ecs_task_definition" "gitsync_task_definition" {
  family                   = "${local.name_prefix}-taskdef"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  container_definitions = jsonencode([
    {
      essential = true
      image     = "${aws_ecr_repository.ecr_repo.repository_url}:${var.ecr_image_version}"
      name      = "${local.name_prefix}-container-definition"
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-region        = "${var.region}"
          awslogs-group         = aws_cloudwatch_log_group.ecs_lg.name
          awslogs-stream-prefix = "ecs"
        }
      }
      environment = [
        {
          "name" : "REGION",
          "value" : "${var.region}"
        },
        {
          "name" : "ENVIRONMENT",
          "value" : "${var.environment}"
        },
        {
          "name" : "RDS_URL_SSM_PARAMETER_NAME",
          "value" : "${var.rds_url_ssm_parameter_name}"
        },
        {
          "name" : "RDS_USERNAME_SSM_PARAMETER_NAME",
          "value" : "${var.rds_username_ssm_parameter_name}"
        },
        {
          "name" : "RDS_PASSWORD_SSM_PARAMETER_NAME",
          "value" : "${var.rds_password_ssm_parameter_name}"
        },
        {
          "name" : "TOKEN_REGISTRY_REPOSITORY_URL",
          "value" : "https://github.com/cardano-foundation/cardano-token-registry.git"
        }
      ]
    }
  ])
}

data "aws_iam_policy_document" "gitsync_cw_event_role_assume_role_policy" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      identifiers = ["events.amazonaws.com"]
      type        = "Service"
    }
  }
}

data "aws_iam_policy_document" "gitsync_cw_event_role_cloudwatch_policy" {
  statement {
    effect    = "Allow"
    actions   = ["ecs:RunTask"]
    resources = [aws_ecs_task_definition.gitsync_task_definition.arn]
  }
  statement {
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.ecs_execution_role.arn, aws_iam_role.ecs_task_role.arn]
  }
}

resource "aws_iam_role" "gitsync_cw_event_role" {
  name               = "${local.iam_name_prefix}-cw-role"
  assume_role_policy = data.aws_iam_policy_document.gitsync_cw_event_role_assume_role_policy.json
}

resource "aws_iam_role_policy" "gitsync_cw_event_role_cloudwatch_policy" {
  name   = "${local.iam_name_prefix}-cw-policy"
  role   = aws_iam_role.gitsync_cw_event_role.id
  policy = data.aws_iam_policy_document.gitsync_cw_event_role_cloudwatch_policy.json
}

resource "aws_cloudwatch_event_rule" "event_rule" {
  name                = "${local.iam_name_prefix}-event-rule"
  schedule_expression = var.event_rule_schedule_expression
  description         = var.event_rule_description
  is_enabled          = true
}

resource "aws_cloudwatch_event_target" "gitsync_scheduled_task" {
  rule           = aws_cloudwatch_event_rule.event_rule.name
  event_bus_name = aws_cloudwatch_event_rule.event_rule.event_bus_name
  arn            = aws_ecs_cluster.gitsync_service_cluster.arn
  role_arn       = aws_iam_role.gitsync_cw_event_role.arn

  ecs_target {
    launch_type         = "FARGATE"
    task_definition_arn = aws_ecs_task_definition.gitsync_task_definition.arn
    propagate_tags      = "TASK_DEFINITION"

    network_configuration {
      subnets          = var.ecs_target_subnets
      security_groups  = concat(var.ecs_target_security_groups, [aws_security_group.gitsync_service_sg.id])
      assign_public_ip = var.ecs_target_assign_public_ip
    }
  }
}