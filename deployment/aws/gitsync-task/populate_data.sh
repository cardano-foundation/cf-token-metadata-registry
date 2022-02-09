if [[ -z "${METADATA_DB_NAME}" ]]; then
  METADATA_DB_NAME="cf_metadata"
fi
if [[ -z "${DB_MIGRATION_SCRIPTS_FOLDER}" ]]; then
  DB_MIGRATION_SCRIPTS_FOLDER="./"
fi
if [[ -z "${DB_PORT}" ]]; then
  DB_PORT="5432"
  fi
if [[ -z "${DB_HOST}" ]]; then
  DB_HOST="localhost"
fi
if [[ -z "${ENVIRONMENT}" ]]; then
  ENVIRONMENT="dev"
fi
if [[ -z "${REGISTRY_CLONE_FOLDER}" ]]; then
  REGISTRY_CLONE_FOLDER="../../../java/api/tmp/cardano-token-registry/"
fi

# fetch secrets from secrets manager.
if [[ -z "${DBA_USER_SECRET}" ]]; then
  DB_MASTER_USER_SECRET_ARN="$(aws secretsmanager list-secrets | jq .SecretList | jq -c 'map(select(.Name | contains("cf-metadata-api-'${ENVIRONMENT}'-db-master-user")))' | jq -r .[0].ARN)"
  DBA_USER_SECRET="$(aws secretsmanager get-secret-value --secret-id "${DB_MASTER_USER_SECRET_ARN}" | jq -r .SecretString | jq -r .password)"
  DBA_USER_NAME="$(aws secretsmanager get-secret-value --secret-id "${DB_MASTER_USER_SECRET_ARN}" | jq -r .SecretString | jq -r .username)"
fi
if [[ -z "${DBA_USER_NAME}" ]]; then
  DB_MASTER_USER_SECRET_ARN="$(aws secretsmanager list-secrets | jq .SecretList | jq -c 'map(select(.Name | contains("cf-metadata-api-'${ENVIRONMENT}'-db-master-user")))' | jq -r .[0].ARN)"
  DBA_USER_NAME="$(aws secretsmanager get-secret-value --secret-id "${DB_MASTER_USER_SECRET_ARN}" | jq -r .SecretString | jq -r .username)"
fi

pip install -r "${DB_MIGRATION_SCRIPTS_FOLDER}requirements.txt"
python "${DB_MIGRATION_SCRIPTS_FOLDER}populate_data.py" --dbhost ${DB_HOST} --dbport ${DB_PORT} --dbuser ${DBA_USER_NAME} --dbsecret ${DBA_USER_SECRET} --dbname ${METADATA_DB_NAME} --mappings "${REGISTRY_CLONE_FOLDER}mappings"
