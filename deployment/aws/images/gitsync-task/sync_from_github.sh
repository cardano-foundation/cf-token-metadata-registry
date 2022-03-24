#!/bin/bash

# 1. checkout repo
CLONE_FOLDER="registry-data"
echo "Populating data from Github into the database ..."
git clone ${TOKEN_REGISTRY_REPOSITORY_URL} "${CLONE_FOLDER}"
cd "${CLONE_FOLDER}"
git checkout master
git pull
pwd
cd ..

python populate_data.py --awsssm --mappings "/cf-gitsync-job/${CLONE_FOLDER}/mappings"
