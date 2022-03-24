locals {
  name_prefix = var.name_prefix
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name = "vpc-id"
    values = [
      data.aws_vpc.default.id
    ]
  }
}

resource "aws_cloudwatch_log_group" "ecs_lg" {
  name = "${local.name_prefix}-ecs-logs"
}

resource "aws_cloudwatch_log_group" "ecs_cluster_lg" {
  name = "${local.name_prefix}-ecs-cluster-logs"
}

resource "aws_ecs_cluster" "cluster" {
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

resource "aws_security_group" "task_sg" {
  name        = "${local.name_prefix}-sg"
  description = "Security group for ECS tasks running the laodtest tasks."
  vpc_id      = data.aws_vpc.default.id

  egress {
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "-1"
  }
}

resource "aws_ecs_task_definition" "task_definition" {
  family                   = "${local.name_prefix}-taskdef"
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.task_role_arn
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  container_definitions = jsonencode([
    {
      essential = true
      image     = "${var.ecr_repository_url}:${var.ecr_image_version}"
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
          "name" : "ENVIRONMENT",
          "value" : "${var.environment}"
        },
        {
          "name" : "REGION",
          "value" : "${var.region}"
        },
        {
          "name" : "LOAD_TEST_RESULT_BUCKET_NAME",
          "value" : "${var.loadtest_bucket.id}"
        },
        {
          "name" : "ECS_ENABLE_CONTAINER_METADATA",
          "value" : "true"
        },
        {
          "name" : "LOAD_TEST_ID",
          "value" : "0"
        },
      ]
    }
  ])
}
