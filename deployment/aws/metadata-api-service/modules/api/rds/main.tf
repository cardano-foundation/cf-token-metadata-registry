resource "random_password" "master_password" {
  length           = 20
  special          = true
  override_special = "_%!?:;#"
}

resource "aws_secretsmanager_secret" "master_user_secret" {
  name = "${var.name_prefix}-db-master-user"
}

resource "aws_secretsmanager_secret_version" "master_user_secret_version" {
  secret_id = aws_secretsmanager_secret.master_user_secret.id
  secret_string = jsonencode({
    username = "${var.admin_user_name}",
    password = "${random_password.master_password.result}"
  })

  lifecycle {
    ignore_changes = [
      secret_string,
    ]
  }
}

data "aws_secretsmanager_secret" "master_user_secret" {
  arn = aws_secretsmanager_secret_version.master_user_secret_version.arn
}

data "aws_secretsmanager_secret_version" "current_master_user_secret" {
  secret_id = data.aws_secretsmanager_secret.master_user_secret.id
}

resource "aws_db_subnet_group" "metadata_db_subnet_group" {
  name       = "${var.name_prefix}-db-sn-grp"
  subnet_ids = var.db_subnet_ids

  tags = {
    Name = "${var.name_prefix}-db-sn-grp"
  }
}

resource "aws_db_parameter_group" "metadata_db_pg" {
  name   = "${var.name_prefix}-db-pg"
  family = "postgres13"

  parameter {
    name  = "log_connections"
    value = "1"
  }
}

resource "aws_db_parameter_group" "metadata_db_pg14" {
  name   = "${var.name_prefix}-db-pg14"
  family = "postgres14"

  parameter {
    name  = "log_connections"
    value = "1"
  }
}

resource "aws_kms_key" "rds_key" {
  description = "This key is used to encrypt the rds instance"

  tags = {
    Name = "${var.name_prefix}-rds-key"
  }
}

resource "aws_kms_alias" "rds_key_alias" {
  name          = "alias/${var.name_prefix}-rds-key"
  target_key_id = aws_kms_key.rds_key.key_id
}

resource "aws_kms_key" "bastion_key" {
  description = "This key is used to encrypt the bastion ebs volume"

  tags = {
    Name = "${var.name_prefix}-bastion-key"
  }
}

resource "aws_kms_alias" "bastion_key_alias" {
  name          = "alias/${var.name_prefix}-bastion-key"
  target_key_id = aws_kms_key.bastion_key.key_id
}

resource "aws_security_group" "access_to_rds_sg" {
  name        = "${var.name_prefix}-access-to-rds-sg"
  description = "Whitelisted for DB access"
  vpc_id      = var.vpc_id
}

resource "aws_security_group" "metadata_db_sg" {
  name        = "${var.name_prefix}-rds-sg"
  description = "Allow all inbound traffic"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = var.port
    to_port         = var.port
    protocol        = "tcp"
    security_groups = [aws_security_group.access_to_rds_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "-1"
  }
}

resource "aws_security_group" "bastion_sg" {
  name        = "${var.name_prefix}-bastion-sg"
  description = "Allow ssh to bastion host"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 22
    to_port     = 22
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

resource "aws_db_instance" "metadata_db" {
  identifier                      = "${var.instance_name}-${var.environment}"
  instance_class                  = var.instance_type
  engine                          = "postgres"
  engine_version                  = "14.1"
  port                            = var.port
  username                        = var.admin_user_name
  password                        = jsondecode(data.aws_secretsmanager_secret_version.current_master_user_secret.secret_string)["password"]
  db_subnet_group_name            = aws_db_subnet_group.metadata_db_subnet_group.name
  vpc_security_group_ids          = [aws_security_group.metadata_db_sg.id]
  parameter_group_name            = aws_db_parameter_group.metadata_db_pg14.name
  publicly_accessible             = var.publicly_accessible
  multi_az                        = var.multi_az
  skip_final_snapshot             = true
  allow_major_version_upgrade     = true
  apply_immediately               = true
  allocated_storage               = var.min_storage
  max_allocated_storage           = var.max_storage
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  storage_encrypted               = true
  kms_key_id                      = aws_kms_key.rds_key.arn
}

resource "aws_key_pair" "bastion_key_pair" {
  key_name   = "${var.name_prefix}-bastion-key-pair"
  public_key = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIB0sRtFDDH4mCgTSC3UPRVgzGHatDfJFxkasxcNv3rfH"
}

resource "aws_iam_instance_profile" "bastion_profile" {
  name = "${var.name_prefix}-${var.region}-bastion-instance-profile"
  role = aws_iam_role.bastion_role.name
}

resource "aws_iam_role" "bastion_role" {
  name                = "${var.name_prefix}-${var.region}-bastion_role"
  path                = "/"
  managed_policy_arns = ["arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore", aws_iam_policy.bastion_role_policy.arn]
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Principal = {
          Service = "ec2.amazonaws.com"
        },
        Effect = "Allow",
        Sid    = ""
      }
    ]
  })
}

resource "aws_iam_policy" "bastion_role_policy" {
  name        = "${var.name_prefix}-${var.region}-bastion-role-policy"
  description = "Policy for the bastion host"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "s3:*",
          "kms:*",
          "rds:*",
        ]
        Effect   = "Allow"
        Resource = "*"
      },
      {
        Action = [
          "ssmmessages:CreateControlChannel",
          "ssmmessages:CreateDataChannel",
          "ssmmessages:OpenControlChannel",
          "ssmmessages:OpenDataChannel"
        ]
        Effect   = "Allow"
        Resource = "*"
      }
    ]
  })
}

data "aws_ami" "amazon_linux_2" {
  most_recent = true

  filter {
    name = "name"
    values = ["amzn2-ami-hvm-*-x86_64-ebs"]
  }
  owners = ["amazon"]
}

resource "aws_instance" "bastion" {
  ami                    = data.aws_ami.amazon_linux_2.id
  key_name               = aws_key_pair.bastion_key_pair.key_name
  instance_type          = "t2.micro"
  vpc_security_group_ids = ["${aws_security_group.bastion_sg.id}", "${aws_security_group.access_to_rds_sg.id}"]
  subnet_id              = element(var.app_subnet_ids, 0)
  iam_instance_profile   = aws_iam_instance_profile.bastion_profile.name
  hibernation            = false
  ebs_block_device {
    kms_key_id  = aws_kms_key.bastion_key.arn
    volume_size = 30
    volume_type = "gp2"
    encrypted   = true
    device_name = "/dev/xvda"
  }

  tags = {
    Name = "${var.name_prefix}-bastionhost"
  }

  lifecycle {
    ignore_changes = [
      ebs_block_device,
    ]
  }
}
