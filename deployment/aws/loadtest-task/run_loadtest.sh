#!/bin/bash
if [[ -z "${ENVIRONMENT}" ]]; then
  ENVIRONMENT=dev
fi

artillery run -e "${ENVIRONMENT}" -o /loadtest/result.json /loadtest/loadtest.yaml
md5_result_file=`md5sum /loadtest/result.json | awk '{ print $1 }'`
aws s3 cp /loadtest/result.json "s3://${LOAD_TEST_RESULT_BUCKET_NAME}/results/${LOAD_TEST_ID}/${REGION}/result_${md5_result_file}.json"