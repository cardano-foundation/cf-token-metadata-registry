#!/bin/bash
if [[ -z "${AWS_ACCOUNT_ID}" ]]; then
  AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
fi
if [[ -z "${ENVIRONMENT}" ]]; then
  ENVIRONMENT="dev"
fi
if [[ -z "${REGION1}" ]]; then
  REGION1="eu-central-1"
fi
if [[ -z "${REGION2}" ]]; then
  REGION2="us-east-1"
fi
if [[ -z "${REGION3}" ]]; then
  REGION3="ap-southeast-1"
fi

./push_to_ecr.sh --region ${REGION1} --accountid ${AWS_ACCOUNT_ID} --dockerfolder "images/monitoring-lambda" --reponame "cf-metadata-api-ecr-${ENVIRONMENT}-monitoring-${REGION1}-image-repo"
./push_to_ecr.sh --region ${REGION2} --accountid ${AWS_ACCOUNT_ID} --dockerfolder "images/monitoring-lambda" --reponame "cf-metadata-api-ecr-${ENVIRONMENT}-monitoring-${REGION2}-image-repo"
./push_to_ecr.sh --region ${REGION3} --accountid ${AWS_ACCOUNT_ID} --dockerfolder "images/monitoring-lambda" --reponame "cf-metadata-api-ecr-${ENVIRONMENT}-monitoring-${REGION3}-image-repo"
