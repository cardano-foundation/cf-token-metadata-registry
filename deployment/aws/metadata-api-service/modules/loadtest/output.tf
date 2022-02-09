output "ecr_repository_url" {
  value = aws_ecr_repository.loadtest_ecr_repo.repository_url
}

output "loadtest_bucket_id" {
  value = aws_s3_bucket.loadtest_bucket.id
}

output "loadtest_bucket_arn" {
  value = aws_s3_bucket.loadtest_bucket.arn
}

output "deployment_package_id" {
  value = aws_s3_bucket_object.lambda_test_runner_deployment_package.key
}