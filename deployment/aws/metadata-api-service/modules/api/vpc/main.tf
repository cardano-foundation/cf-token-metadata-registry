data "aws_availability_zones" "available" {}

resource "aws_vpc" "vpc" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "vpc-${var.environment}"
  }
}

resource "aws_subnet" "pub_subnet" {
  count                   = length(data.aws_availability_zones.available.names)
  vpc_id                  = aws_vpc.vpc.id
  cidr_block              = replace(replace(var.vpc_cidr, "/16", "/24"), ".0.0", ".${10 + count.index}.0")
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "pub-${data.aws_availability_zones.available.names[count.index]}-${var.environment}"
    AZ = "${data.aws_availability_zones.available.names[count.index]}"
  }
}

resource "aws_subnet" "app_subnet" {
  count                   = length(data.aws_availability_zones.available.names)
  vpc_id                  = aws_vpc.vpc.id
  cidr_block              = replace(replace(var.vpc_cidr, "/16", "/24"), ".0.0", ".${20 + count.index}.0")
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = false

  tags = {
    Name = "app-${data.aws_availability_zones.available.names[count.index]}-${var.environment}"
    AZ = "${data.aws_availability_zones.available.names[count.index]}"
  }
}

resource "aws_subnet" "db_subnet" {
  count                   = length(data.aws_availability_zones.available.names)
  vpc_id                  = aws_vpc.vpc.id
  cidr_block              = replace(replace(var.vpc_cidr, "/16", "/24"), ".0.0", ".${30 + count.index}.0")
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = false

  tags = {
    Name = "db-${data.aws_availability_zones.available.names[count.index]}-${var.environment}"
    AZ = "${data.aws_availability_zones.available.names[count.index]}"
  }
}

resource "aws_internet_gateway" "internet_gw" {
  vpc_id = aws_vpc.vpc.id

  tags = {
    Name = "ig-${var.environment}"
  }
}

resource "aws_eip" "nat_eip" {
  vpc = true
}

resource "aws_nat_gateway" "nat_gw" {
  allocation_id = aws_eip.nat_eip.id
  subnet_id     = element(aws_subnet.pub_subnet.*.id, 1)
  depends_on    = [aws_internet_gateway.internet_gw]
}

resource "aws_route_table" "pub_rtbl" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.internet_gw.id
  }
}

resource "aws_route_table_association" "pub_rtbla" {
  count          = length(data.aws_availability_zones.available.names)
  subnet_id      = element(aws_subnet.pub_subnet.*.id, count.index)
  route_table_id = aws_route_table.pub_rtbl.id
}

resource "aws_route_table" "app_rtbl" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat_gw.id
  }
}

resource "aws_route_table_association" "app_rtbla" {
  count          = length(data.aws_availability_zones.available.names)
  subnet_id      = element(aws_subnet.app_subnet.*.id, count.index)
  route_table_id = aws_route_table.app_rtbl.id
}

resource "aws_route_table" "db_rtbl" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat_gw.id
  }
}

resource "aws_route_table_association" "db_rtbla" {
  count          = length(data.aws_availability_zones.available.names)
  subnet_id      = element(aws_subnet.db_subnet.*.id, count.index)
  route_table_id = aws_route_table.db_rtbl.id
}

resource "aws_security_group" "default" {
  name        = "default-sg-${var.environment}"
  description = "Default security group to allow inbound/outbound from the ${var.environment} VPC"
  vpc_id      = aws_vpc.vpc.id
  depends_on  = [aws_vpc.vpc]

  ingress {
    from_port = "0"
    to_port   = "0"
    protocol  = "-1"
    self      = true
  }

  egress {
    from_port = "0"
    to_port   = "0"
    protocol  = "-1"
    self      = "true"
  }
}
