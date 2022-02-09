from datetime import datetime
import logging
import os
import time
import boto3

logger = logging.getLogger()
logger.setLevel(level=logging.INFO)

def run_task(task_definition_arn, cluster_arn, security_group_id, subnet_id, ecs, env_override=None):
    try:
        overrides = { "containerOverrides": [ ] }
        if env_override:
            describe_task_response = ecs.describe_task_definition(taskDefinition=task_definition_arn)
            if describe_task_response['taskDefinition'] and describe_task_response['taskDefinition']['containerDefinitions'] and len(describe_task_response['taskDefinition']['containerDefinitions']) > 0:
                new_task_env = describe_task_response['taskDefinition']['containerDefinitions'][0]['environment']
                for key in env_override:
                    found = False
                    for env_pair in new_task_env:
                        if env_pair['name'] == key:
                            env_pair['value'] = str(env_override[key])
                            found = True
                            break
                    if found == False:
                        new_task_env.append({'name': key, 'value': str(env_override[key])})

                overrides = {
                    'containerOverrides': [
                        {
                            'name': describe_task_response['taskDefinition']['containerDefinitions'][0]['name'],
                            'environment': new_task_env
                        }
                    ]
                }
                
        run_task_response = ecs.run_task(
            cluster = cluster_arn,
            launchType = 'FARGATE',
            networkConfiguration={
                'awsvpcConfiguration': {
                    'subnets': [
                        subnet_id,
                    ],
                    'securityGroups': [
                        security_group_id,
                    ],
                    'assignPublicIp': 'ENABLED'
                }
            },
            overrides=overrides,
            taskDefinition = task_definition_arn
        )
        if len(run_task_response['tasks']) > 0:
            return run_task_response['tasks'][0]['taskArn']
    except Exception as e:
        logging.error('Could not execute task.')
        logging.exception(e)
        raise e

def wait_for_tasks_status(tasks, interval_seconds, retries, desired_status):
    all_tasks_in_desired_status = False
    task_states = {}
    tasks_ok = 0
    retry = 0
    while not all_tasks_in_desired_status and retry < retries:
        try:
            for task in tasks:
                if task["task"] not in task_states or task_states[task["task"]] != True:
                    response = task["ecs_client"].describe_tasks(cluster=task["cluster_arn"], tasks=[task["task"]])
                    if len(response['tasks']) == 1:
                        task_states[task["task"]] = response['tasks'][0]['lastStatus'] in desired_status
            tasks_ok = 0
            for task in tasks:
                if task["task"] in task_states and task_states[task["task"]] == True:
                    tasks_ok += 1
            all_tasks_in_desired_status = len(tasks) == tasks_ok
        except Exception as e:
            logging.error('Error while waiting for task to reach desired status.')
            logging.exception(e)
        finally:
            if not all_tasks_in_desired_status:
                retry = retry + 1
                logging.info(f'Waiting for tasks {len(tasks) - tasks_ok} of {len(tasks)} to reach desired status. Retry {retry} of {retries}.')
                time.sleep(interval_seconds)

def handler(event, context):
    try:
      run_id = datetime.now().strftime("%Y_%m_%d-%H_%M_%S")
      env_dict = dict()
      env_dict['LOAD_TEST_ID'] = run_id
      region_config = [tuple(region_config_entry.split(":")) for region_config_entry in os.getenv("LOADTEST_REGION_CONFIG", "eu-central-1:1").split(",")]
      tasks = []
      logging.info(f"Starting loadtest tasks in target regions: {region_config}")
      for loadtest_config_entry in region_config:
        task_definition_arn = os.getenv(f"ECS_TASK_DEFINITION_ARN_{loadtest_config_entry[0]}")
        cluster_arn = os.getenv(f"CLUSTER_ARN_{loadtest_config_entry[0]}")
        security_group_id = os.getenv(f"SECURITY_GROUP_ID_{loadtest_config_entry[0]}")
        subnet_id = os.getenv(f"SUBNET_ID_{loadtest_config_entry[0]}")
        env_dict["REGION"] = os.getenv(f"REGION_{loadtest_config_entry[0]}")
        ecs_client = boto3.client('ecs', env_dict["REGION"])
        for i in range(int(loadtest_config_entry[1])):
          tasks.append({
              "ecs_client": ecs_client,
              "task": run_task(task_definition_arn, cluster_arn, security_group_id, subnet_id, ecs_client, env_dict),
              "cluster_arn": cluster_arn
          })

      logging.info("Waiting for all tasks to enter at least RUNNING state ...")
      wait_for_tasks_status(tasks, 15, 120, ['STOPPED', 'RUNNING'])
    except Exception as exc:
      logger.exception(exc)
