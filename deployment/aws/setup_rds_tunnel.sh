#!/bin/bash
BASTION_INSTANCE_ID="UNKNOWN"
LOCAL_PORT="5432"
ENVIRONMENT="dev"
REGION="eu-west-1"

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    -k|--keys)
      BASTION_KEYS_PATH="$2"
      shift # past argument
      shift # past value
      ;;
    -p|--localport)
      LOCAL_PORT="$2"
      shift # past argument
      shift # past value
      ;;
    -e|--environment)
      ENVIRONMENT="$2"
      shift # past argument
      shift # past value
      ;;
    -r|--region)
      REGION="$2"
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

BASTION_INSTANCE_ID="$(AWS_REGION=${REGION} aws ec2 describe-instances | jq .Reservations[].Instances | jq -r -c 'map(select(.Tags[].Value | contains("'${ENVIRONMENT}'-bastionhost"))) | .[].InstanceId')"
RDS_INSTANCE_ID_ADDRESS="$(AWS_REGION=${REGION} aws rds describe-db-instances | jq .DBInstances | jq -c 'map(select(.DBInstanceIdentifier | contains("metadata-cf-'${ENVIRONMENT}'")))' | jq .[0].Endpoint | jq -r .Address)"
RDS_INSTANCE_ID_PORT="$(AWS_REGION=${REGION} aws rds describe-db-instances | jq .DBInstances | jq -c 'map(select(.DBInstanceIdentifier | contains("metadata-cf-'${ENVIRONMENT}'")))' | jq .[0].Endpoint | jq -r .Port)"
AWS_REGION=${REGION} ssh -i ${BASTION_KEYS_PATH} ec2-user@${BASTION_INSTANCE_ID} -L ${LOCAL_PORT}:${RDS_INSTANCE_ID_ADDRESS}:${RDS_INSTANCE_ID_PORT}
