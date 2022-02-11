locals {
  name_prefix     = "${var.name_prefix}-service"
  iam_name_prefix = "${var.name_prefix}-${var.region}-service"
}

resource "aws_kms_key" "ecr_key" {
  description = "This key is used to encrypt the ecr repository of the service API."

  tags = {
    Name = "${local.name_prefix}-ecr-key"
  }
}

resource "aws_kms_alias" "ecr_key_alias" {
  name          = "alias/${local.name_prefix}-ecr-key"
  target_key_id = aws_kms_key.ecr_key.key_id
}

resource "aws_ecr_repository" "service_ecr_repo" {
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

resource "aws_s3_bucket" "lb_log_bucket" {
  bucket = "cf-metadata-${var.environment}-${var.region}-lb-logs"
  acl    = "log-delivery-write"

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
}

resource "aws_s3_bucket_public_access_block" "lb_log_bucket_blocking" {
  bucket = aws_s3_bucket.lb_log_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_policy" "lb_log_bucket_policy" {
  bucket = aws_s3_bucket.lb_log_bucket.id
  policy = data.aws_iam_policy_document.lb_log_bucket_policy_document.json
}

data "aws_elb_service_account" "main" {}

data "aws_iam_policy_document" "lb_log_bucket_policy_document" {
  policy_id = "s3_bucket_lb_logs"

  statement {
    actions = [
      "s3:PutObject",
    ]
    effect = "Allow"
    resources = [
      "${aws_s3_bucket.lb_log_bucket.arn}/*",
    ]

    principals {
      identifiers = ["${data.aws_elb_service_account.main.arn}"]
      type        = "AWS"
    }
  }

  statement {
    actions = [
      "s3:PutObject"
    ]
    effect    = "Allow"
    resources = ["${aws_s3_bucket.lb_log_bucket.arn}/*"]
    principals {
      identifiers = ["delivery.logs.amazonaws.com"]
      type        = "Service"
    }
  }

  statement {
    actions = [
      "s3:GetBucketAcl"
    ]
    effect    = "Allow"
    resources = ["${aws_s3_bucket.lb_log_bucket.arn}"]
    principals {
      identifiers = ["delivery.logs.amazonaws.com"]
      type        = "Service"
    }
  }
}

resource "random_password" "service_password" {
  length           = 20
  special          = true
  override_special = "_%!?:;#"
}

resource "aws_secretsmanager_secret" "service_user_secret" {
  name = "${local.name_prefix}-db-service-user"
}

resource "aws_secretsmanager_secret_version" "service_user_secret_version" {
  secret_id = aws_secretsmanager_secret.service_user_secret.id
  secret_string = jsonencode({
    username = "${var.service_user_name}",
    password = "${random_password.service_password.result}"
  })

  lifecycle {
    ignore_changes = [
      secret_string,
    ]
  }
}

data "aws_secretsmanager_secret" "service_user_secret" {
  arn = aws_secretsmanager_secret_version.service_user_secret_version.arn
}

data "aws_secretsmanager_secret_version" "current_service_user_secret" {
  secret_id = data.aws_secretsmanager_secret.service_user_secret.id
}

resource "aws_ssm_parameter" "rds_url" {
  name        = "/${var.project}/${var.environment}/rds/url"
  description = "The url to connect to the RDS instance used for ${var.project} ${var.environment} environment."
  type        = "SecureString"
  value       = "jdbc:postgresql://${var.rds_instance_endpoint}/${var.metadata_db_name}"
}

resource "aws_ssm_parameter" "rds_username" {
  name        = "/${var.project}/${var.environment}/rds/service-user-name"
  description = "The name of the RDS user for the metadata api service used for ${var.project} ${var.environment} environment."
  type        = "SecureString"
  value       = var.service_user_name
}

resource "aws_ssm_parameter" "rds_password" {
  name        = "/${var.project}/${var.environment}/rds/service-user-secret"
  description = "The password of the RDS user for the metadata api service used for ${var.project} ${var.environment} environment."
  type        = "SecureString"
  value       = jsondecode(data.aws_secretsmanager_secret_version.current_service_user_secret.secret_string)["password"]
}

resource "aws_ssm_parameter" "rds_driver_class_name" {
  name        = "/${var.project}/${var.environment}/rds/driver-class-name"
  description = "The password of the RDS user for the metadata api service used for ${var.project} ${var.environment} environment."
  type        = "SecureString"
  value       = var.database_driver_class_name
}

resource "aws_cloudwatch_log_group" "ecs_cluster_lg" {
  name = "${local.name_prefix}-ecs-cluster-logs"
}

resource "aws_cloudwatch_log_group" "ecs_service_lg" {
  name = "${local.name_prefix}-ecs-logs"
}

resource "aws_ecs_cluster" "api_service_cluster" {
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
  description = "Policy for the ecs task of the API service."
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

resource "aws_ecs_task_definition" "api_service_task_definition" {
  family                   = "${local.name_prefix}-taskdef"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.service_task_cpu
  memory                   = var.service_task_memory
  container_definitions = jsonencode([
    {
      essential = true
      image     = "${aws_ecr_repository.service_ecr_repo.repository_url}:${var.service_image_version}"
      name      = "${local.name_prefix}-container-definition"
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-region        = "${var.region}"
          awslogs-group         = aws_cloudwatch_log_group.ecs_service_lg.name
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
          "name" : "DB_CONNECTION_PARAMS_PROVIDER_TYPE",
          "value" : "AWS_SSM"
        },
        {
          "name" : "RDS_URL_SSM_PARAMETER_NAME",
          "value" : aws_ssm_parameter.rds_url.name
        },
        {
          "name" : "RDS_USERNAME_SSM_PARAMETER_NAME",
          "value" : aws_ssm_parameter.rds_username.name
        },
        {
          "name" : "RDS_PASSWORD_SSM_PARAMETER_NAME",
          "value" : aws_ssm_parameter.rds_password.name
        },
        {
          "name" : "RDS_DRIVER_CLASS_NAME_SSM_PARAMETER_NAME",
          "value" : aws_ssm_parameter.rds_driver_class_name.name
        }
      ]
      portMappings = [
        {
          containerPort = var.service_task_container_port
          hostPort      = var.service_task_container_port
        }
      ]
    }
  ])
}

resource "aws_appautoscaling_target" "ecs_autoscaling_target" {
  min_capacity       = var.service_task_count_min
  max_capacity       = var.service_task_count_max
  resource_id        = "service/${aws_ecs_cluster.api_service_cluster.name}/${aws_ecs_service.api_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_autoscaling_policy" {
  name               = "${local.iam_name_prefix}-as-policy"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.ecs_autoscaling_target.service_namespace
  scalable_dimension = aws_appautoscaling_target.ecs_autoscaling_target.scalable_dimension
  resource_id        = aws_appautoscaling_target.ecs_autoscaling_target.resource_id

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    scale_in_cooldown  = 180
    scale_out_cooldown = 300
    target_value       = 50
  }
}

resource "aws_security_group" "api_service_sg" {
  name        = "${local.name_prefix}-sg"
  description = "Security group for ECS tasks running as the API backend service."
  vpc_id      = var.vpc_id

  ingress {
    from_port   = var.service_task_container_port
    to_port     = var.service_task_container_port
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "-1"
  }
}

resource "aws_security_group" "api_service_lb_sg" {
  name        = "${local.name_prefix}-lb-sg"
  description = "Security group for loadbalancer of the API backend service."
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "-1"
  }
}

resource "aws_ecs_service" "api_service" {
  name                               = local.name_prefix
  cluster                            = aws_ecs_cluster.api_service_cluster.id
  task_definition                    = aws_ecs_task_definition.api_service_task_definition.arn
  desired_count                      = var.service_task_count_min
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100
  health_check_grace_period_seconds  = 300
  launch_type                        = "FARGATE"

  network_configuration {
    subnets          = var.ecs_target_subnets
    security_groups  = concat(var.ecs_target_security_groups, [aws_security_group.api_service_sg.id])
    assign_public_ip = var.ecs_target_assign_public_ip
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api_service_loadbalancer_targetgroup.arn
    container_name   = "${local.name_prefix}-container-definition"
    container_port   = var.service_task_container_port
  }

  lifecycle {
    ignore_changes = [desired_count]
  }
}

resource "aws_lb" "api_service_loadbalancer" {
  name               = "${local.name_prefix}-lb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.api_service_lb_sg.id]
  subnets            = var.lb_target_subnets
  idle_timeout       = 60
  access_logs {
    bucket  = aws_s3_bucket.lb_log_bucket.id
    prefix  = "lb_logs"
    enabled = var.lb_access_logs_enabled
  }
}

resource "aws_lb_target_group" "api_service_loadbalancer_targetgroup" {
  name                 = "${local.name_prefix}-lbtg"
  port                 = var.service_task_container_port
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = var.vpc_id
  deregistration_delay = 60

  health_check {
    healthy_threshold   = 3
    unhealthy_threshold = 3
    timeout             = 6
    interval            = 30
    protocol            = "HTTP"
    port                = var.service_task_container_port
    path                = "/v2/health"
  }
}

resource "aws_acm_certificate" "service_certificate" {
  domain_name       = "api.${var.domain_name}"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_listener" "api_service_loadbalancer_listener" {
  load_balancer_arn = aws_lb.api_service_loadbalancer.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = aws_acm_certificate.service_certificate.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api_service_loadbalancer_targetgroup.arn
  }
}

data "aws_route53_zone" "service_zone" {
  name = var.domain_name
}

resource "aws_route53_record" "api_service_r53_record" {
  zone_id = data.aws_route53_zone.service_zone.zone_id
  name    = "api.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_lb.api_service_loadbalancer.dns_name
    zone_id                = aws_lb.api_service_loadbalancer.zone_id
    evaluate_target_health = true
  }

  latency_routing_policy {
    region = var.region
  }

  set_identifier = "${local.name_prefix}-${var.region}"
}

resource "aws_route53_record" "api_service_r53_validation_record" {
  for_each = {
    for dvo in aws_acm_certificate.service_certificate.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = data.aws_route53_zone.service_zone.zone_id
}

resource "aws_acm_certificate_validation" "api_service_acm_validation" {
  certificate_arn         = aws_acm_certificate.service_certificate.arn
  validation_record_fqdns = [for record in aws_route53_record.api_service_r53_validation_record : record.fqdn]
}
