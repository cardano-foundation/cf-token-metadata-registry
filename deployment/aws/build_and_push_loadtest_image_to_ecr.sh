#!/bin/bash
if [[ -z "${AWS_ACCOUNT_ID}" ]]; then
  AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
fi
if [[ -z "${REGION}" ]]; then
  REGION="eu-west-1"
fi
if [[ -z "${ENVIRONMENT}" ]]; then
  ENVIRONMENT="dev"
fi

./push_to_ecr.sh --region ${REGION} --accountid ${AWS_ACCOUNT_ID} --dockerfolder "./loadtest-task" --reponame "cf-metadata-api-${ENVIRONMENT}-loadtest-image-repo"
