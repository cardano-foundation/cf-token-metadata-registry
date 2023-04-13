#!/bin/bash

# change to working directory
if [[ -z "${GITSYNC_WORKING_DIR}" ]]; then
  echo "Remaining in current directory ..."
else
  echo "Changing to working directory ..."
  cd "${GITSYNC_WORKING_DIR}"
fi

# populate mainnet registry data
if [[ -z "${TOKEN_REGISTRY_REPOSITORY_URL}" ]]; then
  echo "No mainnet registry source url specified."
else
  # checkout the repo and target branch
  CLONE_FOLDER="registry-data"
  echo "Populating data from Github into the database ..."
  git clone ${TOKEN_REGISTRY_REPOSITORY_URL} "${CLONE_FOLDER}"
  cd "${CLONE_FOLDER}"
  git checkout ${TOKEN_REGISTRY_BRANCH_NAME}
  git pull
  pwd
  cd ..

  # call the database sync job
  python populate_data.py --source="mainnet" --dburl ${DB_URL} --dbuser ${DB_USER_NAME} --dbsecret ${DB_USER_SECRET} --mappings "/cf-gitsync-job/${CLONE_FOLDER}/${MAPPINGS_FOLDER}"
fi

# populate testnet registry data (whereas testnet means "not mainnet")
if [[ -z "${TESTNET_TOKEN_REGISTRY_REPOSITORY_URL}" ]]; then
  echo "No testnet registry source url specified."
else
  #checkout the testnet repo and target branch
  CLONE_FOLDER="registry-data-testnet"
  echo "Populating data from Github into the database ..."
  git clone ${TESTNET_TOKEN_REGISTRY_REPOSITORY_URL} "${CLONE_FOLDER}"
  cd "${CLONE_FOLDER}"
  git checkout ${TESTNET_TOKEN_REGISTRY_BRANCH_NAME}
  git pull
  pwd
  cd ..

  # call the database sync job for the testnet repository data
  python populate_data.py --source="testnet" --dburl ${DB_URL} --dbuser ${DB_USER_NAME} --dbsecret ${DB_USER_SECRET} --mappings "/cf-gitsync-job/${CLONE_FOLDER}/${TESTNET_MAPPINGS_FOLDER}"
fi
