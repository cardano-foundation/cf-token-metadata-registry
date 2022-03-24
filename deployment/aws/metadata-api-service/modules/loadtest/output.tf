output "loadtest_bucket_id" {
  value = aws_s3_bucket.loadtest_bucket.id
}

output "loadtest_bucket_arn" {
  value = aws_s3_bucket.loadtest_bucket.arn
}

output "deployment_package_id" {
  value = aws_s3_object.lambda_test_runner_deployment_package.key
}