#!/bin/bash
printenv

echo AWS_SSH
echo $DB_CONNECTION_PARAMS_PROVIDER_TYPE
echo $REGION
echo $RDS_USERNAME_SSM_PARAMETER_NAME
echo $RDS_PASSWORD_SSM_PARAMETER_NAME
echo $RDS_URL_SSM_PARAMETER_NAME

echo NOT AWS_SSH
echo $DB_URL
echo $DB_USER
echo $DB_SECRET
echo $DB_DRIVER_CLASS_NAME

if [ "${DB_CONNECTION_PARAMS_PROVIDER_TYPE}" = "AWS_SSM" ]
then
  exec java -jar -DdbConnectionParamsProviderType="${DB_CONNECTION_PARAMS_PROVIDER_TYPE}" -Dregion="${REGION}" -DrdsUsernameSsmParameterName="${RDS_USERNAME_SSM_PARAMETER_NAME}" -DrdsPasswordSsmParameterName="${RDS_PASSWORD_SSM_PARAMETER_NAME}" -DrdsUrlSsmParameterName="${RDS_URL_SSM_PARAMETER_NAME}" -DrdsDriverClassNameSsmParameterName="${RDS_DRIVER_CLASS_NAME_SSM_PARAMETER_NAME}" ./app.jar
else
  exec java -jar -DdbUrl="${DB_URL}" -DdbUser="${DB_USER}" -DdbSecret="${DB_SECRET}" -DdbDriverName="${DB_DRIVER_CLASS_NAME}" ./app.jar
fi

