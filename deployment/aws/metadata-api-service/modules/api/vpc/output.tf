output "vpc_id" {
  value = aws_vpc.vpc.id
}

output "pub_subnets_id" {
  value = "${aws_subnet.pub_subnet.*.id}"
}

output "app_subnets_id" {
  value = "${aws_subnet.app_subnet.*.id}"
}

output "db_subnets_id" {
  value = "${aws_subnet.db_subnet.*.id}"
}

output "default_sg_id" {
  value = aws_security_group.default.id
}

output "security_groups_ids" {
  value = ["${aws_security_group.default.id}"]
}

output "public_route_table" {
  value = aws_route_table.pub_rtbl.id
}
