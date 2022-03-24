import argparse
from datetime import datetime
import json
import logging
import os
import subprocess
import timeit
from dateutil import parser
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session
from daos import TempMetadataDAO, TempLogoDAO
import boto3

# process files ins batches of defined
MAX_PROCESSED_FILE_SIZE_OF_SINGLE_BATCH_BYTES = 1024 * \
    1024 * int(os.getenv('BATCH_SIZE_MB', '64'))

argparser = argparse.ArgumentParser(description='Process some integers.')
argparser.add_argument('--verbose', type=str, const='true', default='false',
                       nargs='?', help='verbose output enabled. default false')
argparser.add_argument('--dbhost', type=str, default='localhost',
                       help='the hostname of the database')
argparser.add_argument('--dbport', type=int, default=5432,
                       help='the port of the database')
argparser.add_argument('--dbname', type=str, default='metadata',
                       help='the name of the database that contains the metadata')
argparser.add_argument('--dbuser', type=str, required=False,
                       help='the username used to access the database')
argparser.add_argument('--dbsecret', type=str, required=False,
                       help='the password used to access the database')
argparser.add_argument('--mappings', type=str, required=True,
                       help='the folder containing the mapping files')
argparser.add_argument('--awsssm', type=str, const='true', default='false',
                       nargs='?', help='fetch database connection infos from AWS SSM parameterstore.')

args = argparser.parse_args()

if args.verbose.lower() == 'true':
    logging.basicConfig(level=logging.DEBUG)
else:
    logging.basicConfig(level=logging.INFO)


def create_db_engine(user: str, password: str, url: str) -> Engine:
    """ Create a SQLAlchemy Engine object based on the given connection parameters.
    The connection string will be postgresql+psycopg2://user:password@host:port/dbname

    Args:
        user (str): Username for database connection.
        password (str): Password for database connection.
        url (str): The database url.

    Returns:
        Engine: A SQLAlchemy Engine object holding the connection to the database.
    """
    logging.info('Trying to connect to postgresql+psycopg2://%s:*****@%s', user, url)
    return create_engine(f'postgresql+psycopg2://{user}:{password}@{url}')


def parse_mappings_file_to_dicts(mapping: dict, author: str, updated: datetime) -> tuple:
    """Parses a json file containing metadata mappings

    Args:
        mapping (dict): the dict representing the loaded json file

    Raises:
        ValueError: raised on invalid input

    Returns:
        tuple: a dict containing the json data reformatted for insertion into the target database. first element is the subjects metadata, second one the according logo if any.
    """
    metadata = []
    logo = []
    if 'subject' in mapping:
        subject = mapping['subject']
        metadata.append({
                        'subject': subject,
                        'policy': mapping['policy'] if 'policy' in mapping else None,
                        'name': mapping['name']['value'] if 'name' in mapping and 'value' in mapping['name'] else None,
                        'ticker': mapping['ticker']['value'] if 'ticker' in mapping and 'value' in mapping['ticker'] else None,
                        'url': mapping['url']['value'] if 'url' in mapping and 'value' in mapping['url'] else None,
                        'description': mapping['description']['value'] if 'description' in mapping and 'value' in mapping['description'] else None,
                        'decimals': mapping['decimals']['value'] if 'decimals' in mapping and 'value' in mapping['decimals'] else None,
                        'updated': updated,
                        'updated_by': author,
                        'properties': mapping
                        })
        if 'logo' in mapping:
            if 'value' in mapping['logo']:
                logo.append({
                    'subject': subject,
                    'logo': mapping['logo']['value']
                })
            else:
                logging.warning(
                    f'Logo with no value field within subject {subject}.')
    else:
        raise ValueError('Missing properties for metadata')

    return metadata, logo


def populate_data(db_engine: Engine, mappings_folder_path: str):
    """ Iterate over the given mapping_folder_path and process each json file that might contain metadata information.

    Args:
        db_engine (Engine): The SQLAlchemy Engine object used to connect to the database.
        mappings_folder_path (str): The path of the folder containing the metadata json files.
    """
    try:
        db_session = Session(db_engine)

        start_time = timeit.default_timer()
        logging.info(f'Preparing upload {mappings_folder_path} ...')
        db_session.execute(f'CREATE TEMPORARY TABLE tmp_logo (LIKE logo);')
        db_session.execute(f'CREATE TEMPORARY TABLE tmp_metadata (LIKE metadata);')

        # iterate over each file
        metadata = []
        logos = []
        total_processed_file_size = 0
        files_processed = 0
        storage_processed = 0
        skipped_files = []
        for file in os.listdir(mappings_folder_path):
            try:
                filename = os.fsdecode(file)
                if filename.endswith('.json'):
                    file_path = os.path.join(mappings_folder_path, file)

                    # get git metadata
                    git_metadata = subprocess.Popen(['git', 'log', '-n 1', '--date-order', '--no-merges', '--pretty=format:%aE#-#%aI', filename],
                                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=mappings_folder_path).stdout.read().decode('utf-8').split('#-#')

                    with open(file_path, 'r') as mappings_file:
                        files_processed += 1
                        file_size_bytes = os.stat(file_path).st_size
                        total_processed_file_size += file_size_bytes
                        storage_processed += file_size_bytes
                        mappings_content = json.load(mappings_file)
                        meta, logo = parse_mappings_file_to_dicts(mappings_content, git_metadata[0], parser.isoparse(git_metadata[1]))
                        metadata.extend(meta)
                        logos.extend(logo)
                        if total_processed_file_size >= MAX_PROCESSED_FILE_SIZE_OF_SINGLE_BATCH_BYTES:
                            if len(metadata) > 0:
                                db_session.bulk_insert_mappings(TempMetadataDAO, metadata)
                            if len(logos) > 0:
                                db_session.bulk_insert_mappings(TempLogoDAO, logos)
                            total_processed_file_size = 0
                            metadata = []
                            logos = []
            except ValueError as exc:
                logging.warning(f'Invalid metadata mappings file {file}. Will be skipped.')
                logging.exception(exc)
                if mappings_content:
                    logging.debug(mappings_content)
                skipped_files.append(file)
            except Exception as exc:
                logging.warning(f'Error during parsing of file {file}. Will be skipped.')
                logging.exception(exc)
        if len(metadata) > 0:
            db_session.bulk_insert_mappings(TempMetadataDAO, metadata)
        if len(logos) > 0:
            db_session.bulk_insert_mappings(TempLogoDAO, logos)

        db_session.commit()
        logging.info(f'Duration of data preparation was {(timeit.default_timer() - start_time):.2f} seconds')

        start_time = timeit.default_timer()
        logging.info('Truncating existing data ...')
        db_session.execute('TRUNCATE logo;')
        db_session.execute('TRUNCATE metadata CASCADE;')

        logging.info(f'Inserting new data from folder {mappings_folder_path} ...')
        db_session.execute('INSERT INTO metadata SELECT subject, policy, name, ticker, url, description, decimals, updated, updated_by, properties FROM tmp_metadata;')
        db_session.execute('INSERT INTO logo SELECT * FROM tmp_logo;')

        db_session.commit()
        db_session.close()
        logging.info(f'Duration of data recreation was {(timeit.default_timer() - start_time):.2f} seconds')
        logging.info(f'Done processing {files_processed} files containing {storage_processed/1024/1024:.2f} MB of data. Skipped {len(skipped_files)} files.')
        logging.debug(f'{skipped_files}')
    except Exception as exc:
        logging.error('Could not insert metadata into database.')
        logging.exception(exc)
        if db_session:
            db_session.close()

def get_aws_ssm_parameter(ssm_client, ssm_parameter_name: str) -> str:
    try:
        response = ssm_client.get_parameter(Name=ssm_parameter_name, WithDecryption=True)
        return response['Parameter']['Value']
    except Exception as exc:
        logging.error("Could not read parameter %s from AWS SSM parameter store.", ssm_parameter_name)
        logging.exception(exc)
        return ""

if __name__ == '__main__':
    if args.awsssm.lower() == 'true':
        ssm = boto3.client('ssm')
        db_user = get_aws_ssm_parameter(ssm, os.getenv('RDS_USERNAME_SSM_PARAMETER_NAME'))
        db_secret = get_aws_ssm_parameter(ssm, os.getenv('RDS_PASSWORD_SSM_PARAMETER_NAME'))
        db_url = get_aws_ssm_parameter(ssm, os.getenv('RDS_URL_SSM_PARAMETER_NAME')).replace('jdbc:postgresql://', '')
    else:
        db_user = args.dbuser
        db_secret = args.dbsecret
        db_url = f'{args.dbhost}:{args.dbport}/{args.dbname}'

    db_engine = create_db_engine(db_user, db_secret, db_url)

    populate_data(db_engine, args.mappings)

    db_engine.dispose()
