output "state_table_arn" {
  value = "${aws_dynamodb_table.terraform_state_lock.arn}"
}

output "state_table_name" {
  value = "${aws_dynamodb_table.terraform_state_lock.name}"
}

output "kms_key_id" {
  value = "${aws_kms_key.deployment_kms_key.id}"
}

output "kms_key_arn" {
  value = "${aws_kms_key.deployment_kms_key.arn}"
}

output "state_bucket" {
  value = "${aws_s3_bucket.terraform_deployment.bucket}"
}

output "state_bucket_arn" {
  value = "${aws_s3_bucket.terraform_deployment.arn}"
}
