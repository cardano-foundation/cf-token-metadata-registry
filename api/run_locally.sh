#!/bin/bash

function cleanup()
{
  echo "Stopping running containers and cleaing up environment ..."
  docker -D stop ${DOCKER_API_CONTAINER_NAME} || true && docker rm ${DOCKER_API_CONTAINER_NAME} || true
  docker -D stop ${DOCKER_PG_CONTAINER_NAME} || true && docker rm ${DOCKER_PG_CONTAINER_NAME} || true
  docker network rm ${DOCKER_NETWORK_NAME} || true
}

trap cleanup EXIT

# setup environment variables
export DOCKER_PG_CONTAINER_NAME="cf-metadata-pg"
export DOCKER_API_CONTAINER_NAME="cf-metadata-api"
export DOCKER_NETWORK_NAME='cfmetadata'
export DB_USER_NAME="metadatadbadmin"
export DB_USER_SECRET="metadata1337_"
export SERVICE_USER_NAME="metadataservice"
export SERVICE_USER_SECRET="metadata1337_"
export METADATA_DB_NAME="cf_metadata"
export DEFAULT_DB_NAME="postgres"
export DB_MIGRATION_SCRIPTS_FOLDER="../../cf-metadata-deployment/database/postgres/scripts/"
export DATAPOPULATE_SCRIPTS_FOLDER="../../cf-metadata-deployment/deployment/aws/images/gitsync-task/"
export DB_HOST='localhost'
export DB_PORT='5432'
export API_EXPOSED_PORT='8080'
export API_LOCAL_BIND_PORT='8081'
export TEMP_FOLDER="tmp/"
export IS_LOCAL_DEPLOYMENT="true"
export DB_CONNECTION_PARAMS_PROVIDER_TYPE="ENVIRONMENT"
export DB_DRIVER_CLASS_NAME="org.postgresql.Driver"
export TOKEN_REGISTRY_REPOSITORY_URL="https://github.com/cardano-foundation/cardano-token-registry.git"
export MAPPINGS_FOLDER="mappings"

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    -b|--buildimages)
      BUILD_DOCKER_IMAGES="TRUE"
      shift # past argument
      ;;
    -s|--skipclone)
      SKIP_CLONE="TRUE"
      shift # past argument
      ;;
    -d|--dbonly)
      DB_ONLY_MODE="TRUE"
      shift # past argument
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

if [ "${BUILD_DOCKER_IMAGES}" == "TRUE" ]
then
  docker build -t cardanofoundation.org/metadata-server-api .
fi

echo "Stopping containers if any are running ..."
docker -D stop ${DOCKER_PG_CONTAINER_NAME} || true && docker rm ${DOCKER_PG_CONTAINER_NAME} || true
docker -D stop ${DOCKER_API_CONTAINER_NAME} || true && docker rm ${DOCKER_API_CONTAINER_NAME} || true

echo "Setting up docker network ..."
docker network create ${DOCKER_NETWORK_NAME} || true

echo "Starting database container ..."
docker run --name ${DOCKER_PG_CONTAINER_NAME} --net ${DOCKER_NETWORK_NAME} -p ${DB_PORT}:${DB_PORT} -e POSTGRES_PASSWORD=${DB_USER_SECRET} -e POSTGRES_USER=${DB_USER_NAME} -d ${DEFAULT_DB_NAME}

while ! pg_isready -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER_NAME} -d ${DEFAULT_DB_NAME} -q
do
    echo "$(date) - waiting for database to start ..."
    sleep 2
done

echo "Bootstrapping the database ..."
pushd ${DB_MIGRATION_SCRIPTS_FOLDER}
./bootstrap_database.sh

echo "Applying schema to database ..."
./migrate_database.sh
popd

echo "Populating data from Github into the database ..."
if [ "${SKIP_CLONE}" == "TRUE" ]
then
  echo "Not fetching latest sources from git repo."
else
  rm -rf ${TEMP_FOLDER}
  mkdir -p ${TEMP_FOLDER}
  pushd ${TEMP_FOLDER}
  git clone ${TOKEN_REGISTRY_REPOSITORY_URL}
  pushd cardano-token-registry
  git checkout master
  git pull
  popd
  popd
fi

export REGISTRY_CLONE_FOLDER="${TEMP_FOLDER}cardano-token-registry/"
DB_MIGRATION_SCRIPTS_FOLDER=${DATAPOPULATE_SCRIPTS_FOLDER} ${DATAPOPULATE_SCRIPTS_FOLDER}populate_data.sh

if [ "${DB_ONLY_MODE}" == "TRUE" ]
then
  read -n 1 -p "Any key to exit:" exitval
else
  echo "Starting up API container ..."
  DB_URL="jdbc:postgresql://${DOCKER_PG_CONTAINER_NAME}:${DB_PORT}/${METADATA_DB_NAME}"
  docker run --platform linux/arm64 -p ${API_LOCAL_BIND_PORT}:${API_EXPOSED_PORT} --name ${DOCKER_API_CONTAINER_NAME} --net ${DOCKER_NETWORK_NAME} -e DB_CONNECTION_PARAMS_PROVIDER_TYPE=${DB_CONNECTION_PARAMS_PROVIDER_TYPE} -e DB_URL="${DB_URL}" -e DB_USER="${SERVICE_USER_NAME}" -e DB_SECRET="${SERVICE_USER_SECRET}" -e DB_DRIVER_CLASS_NAME="${DB_DRIVER_CLASS_NAME}" cardanofoundation.org/metadata-server-api
fi
