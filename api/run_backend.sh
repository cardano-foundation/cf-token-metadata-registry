#!/bin/bash
printenv

export DB_USER=postgres
export DB_SECRET=Hoang1412
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=cf_metadata
export DB_DRIVER_CLASS_NAME=org.postgresql.Driver
export GH_PACKAGES_USER_NAME=Sotatek-HoangNguyen9
export GH_PACKAGES_ACCESS_TOKEN=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
echo "Checking config 1"
echo AWS_SSH
echo $DB_CONNECTION_PARAMS_PROVIDER_TYPE
echo $REGION
echo $RDS_USERNAME_SSM_PARAMETER_NAME
echo $RDS_PASSWORD_SSM_PARAMETER_NAME
echo $RDS_URL_SSM_PARAMETER_NAME

echo "Checking config 2"
echo NOT AWS_SSH
echo $DB_URL
echo $DB_USER
echo $DB_SECRET
echo $DB_DRIVER_CLASS_NAME

if [ "${DB_CONNECTION_PARAMS_PROVIDER_TYPE}" = "AWS_SSM" ]
then
  exec java -jar --enable-preview -DdbConnectionParamsProviderType="${DB_CONNECTION_PARAMS_PROVIDER_TYPE}" -Dregion="${REGION}" -DrdsUsernameSsmParameterName="${RDS_USERNAME_SSM_PARAMETER_NAME}" -DrdsPasswordSsmParameterName="${RDS_PASSWORD_SSM_PARAMETER_NAME}" -DrdsUrlSsmParameterName="${RDS_URL_SSM_PARAMETER_NAME}" -DrdsDriverClassNameSsmParameterName="${RDS_DRIVER_CLASS_NAME_SSM_PARAMETER_NAME}" ./app.jar
else
  exec java -jar --enable-preview -DdbUrl="${DB_URL}" -DdbUser="${DB_USER}" -DdbSecret="${DB_SECRET}" -DdbDriverName="${DB_DRIVER_CLASS_NAME}" ./app.jar
fi

