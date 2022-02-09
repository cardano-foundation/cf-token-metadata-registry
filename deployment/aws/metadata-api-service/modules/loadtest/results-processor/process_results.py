import logging
import os

logger = logging.getLogger()
logger.setLevel(level=logging.INFO)

def handler(event, context):
    try:
      logger.info("Results processor lambda got called.")
      os.getenv("RESULT_BUCKET_NAME")
      os.getenv("TEST_RUN_ID")
      # read s3 objects ... iterate over folder etc.
      # read in files from all regions
      # results per task
      # aggregate per region
      # aggregate in total
      # { 
      #   earliest_started, earliest_ended, test_run_id, environment
      #   tasks = [
      #     { task_region, task_id, counters, timing_stats }
      #   ],
      #   region = [
      #     { region_name, counters, timing_stats }
      #   ],
      #   summary = {
      #     counters, timing_stats
      #   }
      # }
    except Exception as exc:
      logger.exception(exc)
