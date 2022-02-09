#!/bin/bash
POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    -r|--region)
      REGION="$2"
      shift # past argument
      shift # past value
      ;;
    -a|--accountid)
      ACCOUNT_ID="$2"
      shift # past argument
      shift # past value
      ;;
    -f|--dockerfolder)
      DOCKER_FOLDER="$2"
      shift # past argument
      shift # past value
      ;;
    -n|--reponame)
      REPOSITORY_NAME="$2"
      shift # past argument
      shift # past value
      ;;
    -*|--*)
      echo "Unknown option $1"
      exit 1
      ;;
    *)
      POSITIONAL_ARGS+=("$1") # save positional arg
      shift # past argument
      ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters

if [[ -z "${ACCOUNT_ID}" ]]; then
  ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
fi
if [[ -z "${REGION}" ]]; then
  REGION=eu-west-1
fi
if [[ -z "${DOCKER_FOLDER}" ]]; then
  DOCKER_FOLDER="."
fi

ECR_REPOSITORY_URL=${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPOSITORY_NAME}

echo "Building container and pushing to ECR repository ${ECR_REPOSITORY_URL}"
docker build -t ${ECR_REPOSITORY_URL} ${DOCKER_FOLDER}
AWS_REGION=${REGION} aws ecr get-login-password | docker login --username AWS --password-stdin ${ECR_REPOSITORY_URL}
docker push ${ECR_REPOSITORY_URL}
