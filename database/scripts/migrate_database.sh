#!/bin/bash

if [[ -z "${METADATA_DB_NAME}" ]]; then
  METADATA_DB_NAME="cf_metadata"
fi
if [[ -z "${DEFAULT_DB_NAME}" ]]; then
  DEFAULT_DB_NAME="postgres"
fi
#if [[ -z "${DB_MIGRATION_SCRIPTS_FOLDER}" ]]; then
DB_MIGRATION_SCRIPTS_FOLDER="./"
#fi
if [[ -z "${DB_PORT}" ]]; then
  DB_PORT="5432"
fi
if [[ -z "${DB_HOST}" ]]; then
  DB_HOST="localhost"
fi
if [[ -z "${ENVIRONMENT}" ]]; then
  ENVIRONMENT="dev"
fi
if [[ -z "${REGION}" ]]; then
  REGION="eu-west-1"
fi
if [[ -z "${DB_MIGRATION_LIQUIBASE_ROOT_CHANGELOG}" ]]; then
  DB_MIGRATION_LIQUIBASE_ROOT_CHANGELOG="../liquibase/metadata.root-changelog.yaml"
fi

# fetch secrets from secrets manager.
if [[ -z "${DBA_USER_SECRET}" ]]; then
  DB_MASTER_USER_SECRET_ARN="$(AWS_REGION=${REGION} aws secretsmanager list-secrets | jq .SecretList | jq -c 'map(select(.Name | contains("cf-metadata-api-'${ENVIRONMENT}'-db-master-user")))' | jq -r .[0].ARN)"
  DBA_USER_SECRET="$(AWS_REGION=${REGION} aws secretsmanager get-secret-value --secret-id "${DB_MASTER_USER_SECRET_ARN}" | jq -r .SecretString | jq -r .password)"
fi
if [[ -z "${DBA_USER_NAME}" ]]; then
  DB_MASTER_USER_SECRET_ARN="$(AWS_REGION=${REGION} aws secretsmanager list-secrets | jq .SecretList | jq -c 'map(select(.Name | contains("cf-metadata-api-'${ENVIRONMENT}'-db-master-user")))' | jq -r .[0].ARN)"
  DBA_USER_NAME="$(AWS_REGION=${REGION} aws secretsmanager get-secret-value --secret-id "${DB_MASTER_USER_SECRET_ARN}" | jq -r .SecretString | jq -r .username)"
fi

# fetch secrets from secrets manager. This assumes there exists a secret with the name "metadata-db-service-user"
if [[ -z "${SERVICE_USER_SECRET}" ]]; then
  DB_SERVICE_USER_SECRET_ARN="$(AWS_REGION=${REGION} aws secretsmanager list-secrets | jq .SecretList | jq -c 'map(select(.Name | contains("cf-metadata-api-'${ENVIRONMENT}'-service-db-service-user")))' | jq -r .[0].ARN)"
  SERVICE_USER_SECRET="$(AWS_REGION=${REGION} aws secretsmanager get-secret-value --secret-id "${DB_SERVICE_USER_SECRET_ARN}" | jq -r .SecretString | jq -r .password)"
fi
if [[ -z "${SERVICE_USER_NAME}" ]]; then
  DB_SERVICE_USER_SECRET_ARN="$(AWS_REGION=${REGION} aws secretsmanager list-secrets | jq .SecretList | jq -c 'map(select(.Name | contains("cf-metadata-api-'${ENVIRONMENT}'-service-db-service-user")))' | jq -r .[0].ARN)"
  SERVICE_USER_NAME="$(AWS_REGION=${REGION} aws secretsmanager get-secret-value --secret-id "${DB_SERVICE_USER_SECRET_ARN}" | jq -r .SecretString | jq -r .username)"
fi

#echo "Running liquibase db migration ..."
liquibase --url jdbc:postgresql://${DB_HOST}:${DB_PORT}/${METADATA_DB_NAME} --username ${DBA_USER_NAME} --password ${DBA_USER_SECRET} --hub-mode off --changeLogFile ${DB_MIGRATION_LIQUIBASE_ROOT_CHANGELOG} update
liquibase --url jdbc:postgresql://${DB_HOST}:${DB_PORT}/${METADATA_DB_NAME} --username ${DBA_USER_NAME} --password ${DBA_USER_SECRET} --hub-mode off --changeLogFile ${DB_MIGRATION_LIQUIBASE_ROOT_CHANGELOG} history

#echo "Running post migration scripts ..."
PGPASSWORD="${DBA_USER_SECRET}" psql --set=cf_metadata_dbname="${METADATA_DB_NAME}" --set=cf_metadata_serviceuser_name="${SERVICE_USER_NAME}" --set=cf_metadata_serviceuser_secret="${SERVICE_USER_SECRET}" -U "${DBA_USER_NAME}" -h "${DB_HOST}" -p ${DB_PORT} -d ${METADATA_DB_NAME} -f "${DB_MIGRATION_SCRIPTS_FOLDER}post_migration.sql"
