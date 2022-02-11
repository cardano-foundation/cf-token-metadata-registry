import logging
import os
import pycurl
from io import BytesIO
import boto3

logger = logging.getLogger()
logger.setLevel(level=logging.INFO)

region = os.getenv("REGION")
base_path = os.getenv("BASE_PATH")
probing_subject = os.getenv("PROBING_SUBJECT")
connect_timeout = os.getenv("CONNECT_TIMEOUT", "5000")
cloudwatch_region = os.getenv('CLOUDWATCH_REGION')
deployment_environment = os.getenv("DEPLOYMENT_ENVIRONMENT")

cloudwatch = boto3.client('cloudwatch', cloudwatch_region)

def create_metric_entry(metric_name: str, region: str, request_type: str, unit: str, value):
  return {
            'MetricName': metric_name,
            'Dimensions': [
              {
                  'Name' : 'request_type',
                  'Value' : request_type
              },
              {
                  'Name' : 'region',
                  'Value' : region
              },
              {
                  'Name' : 'environment',
                  'Value' : deployment_environment
              }
            ],
            'Value': value,
            'Unit': unit
          }

def collect_metric(url: str, request_type: str):
  buffer = BytesIO()
  c = pycurl.Curl()
  #c.setopt(pycurl.URL, "https://api.metadata.dev.cf-deployments.org/metadata/9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145")
  c.setopt(pycurl.URL, url)
  c.setopt(pycurl.WRITEDATA, buffer)
  c.setopt(pycurl.CONNECTTIMEOUT_MS, int(connect_timeout))
  c.perform()
  buffer.getvalue()

  logging.info("Publishing metrics ...")
  cloudwatch.put_metric_data(
    Namespace = 'metadata-api-monitoring',
    MetricData = [
      create_metric_entry('total_request_duration', region, request_type, 'Seconds', c.getinfo(pycurl.TOTAL_TIME)),
      create_metric_entry('dns_lookup_duration', region, request_type, 'Seconds', c.getinfo(pycurl.NAMELOOKUP_TIME)),
      create_metric_entry('connect_duration', region, request_type, 'Seconds', c.getinfo(pycurl.CONNECT_TIME) - c.getinfo(pycurl.NAMELOOKUP_TIME)),
      create_metric_entry('tls_handshake_duration', region, request_type, 'Seconds', c.getinfo(pycurl.APPCONNECT_TIME) - c.getinfo(pycurl.CONNECT_TIME)),
      create_metric_entry('backend_compute_duration', region, request_type, 'Seconds', c.getinfo(pycurl.PRETRANSFER_TIME) - c.getinfo(pycurl.APPCONNECT_TIME)),
      create_metric_entry('transfer_init_duration', region, request_type, 'Seconds', c.getinfo(pycurl.STARTTRANSFER_TIME) - c.getinfo(pycurl.PRETRANSFER_TIME)),
      create_metric_entry('transfer_download_duration', region, request_type, 'Seconds', c.getinfo(pycurl.TOTAL_TIME) - c.getinfo(pycurl.STARTTRANSFER_TIME)),
      create_metric_entry('redirect_count', region, request_type, 'Count', c.getinfo(pycurl.REDIRECT_COUNT)),
      create_metric_entry('response_code', region, request_type, 'None', c.getinfo(pycurl.RESPONSE_CODE)),
    ]
  )

# see https://curl.se/libcurl/c/curl_easy_getinfo.html for an explanation of the time values
def handler(event, context):
    try:
      collect_metric(f'{base_path}/v2/health', 'v2-metadata-health')
      collect_metric(f'{base_path}/v2/subjects/{probing_subject}', 'v2-subjects')
      collect_metric(f'{base_path}/v2/subjects?limit=5&q=Coin', 'v2-subjects-fulltext')
      collect_metric(f'{base_path}/metadata/{probing_subject}', 'v1-metadata-request')
      collect_metric(f'{base_path}/metadata/{probing_subject}/properties/name', 'v1-metadata-property-request')
    except Exception as exc:
      logger.exception(exc)

handler(None, None)
