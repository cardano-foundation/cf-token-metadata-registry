import argparse
from datetime import datetime
import json
import logging
import os
import subprocess
import timeit
import time
from dateutil import parser
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session
from sqlalchemy.sql import text
from daos import get_metadata_table_model_instance, get_logo_table_model_instance, SyncControlDAO

# process files ins batches of defined
MAX_PROCESSED_FILE_SIZE_OF_SINGLE_BATCH_BYTES = 1024 * \
    1024 * int(os.getenv('BATCH_SIZE_MB', '64'))

argparser = argparse.ArgumentParser(description='Process some integers.')
argparser.add_argument('--verbose', type=str, const='true', default='false',
                       nargs='?', help='verbose output enabled. default false')
argparser.add_argument('--source', type=str, required=True,
                       nargs='?', help='the name of the source for this sync')
argparser.add_argument('--dburl', type=str, required=False,
                       nargs='?', help='the database url containing host, port and the database name')
argparser.add_argument('--dbuser', type=str, required=False,
                       help='the username used to access the database')
argparser.add_argument('--dbsecret', type=str, required=False,
                       help='the password used to access the database')
argparser.add_argument('--mappings', type=str, required=True,
                       help='the folder containing the mapping files')

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
    logging.info(
        'Trying to connect to postgresql+psycopg2://%s:*****@%s', user, url)
    return create_engine(f'postgresql+psycopg2://{user}:{password}@{url}')


def validate_mappings_file_contents(mapping: dict) -> bool:
    """ Check if an entry is valid.
    Check the minimum validity requirements of an entry. This is looser than defined withint CIP-26. Further verification can be done in the downstream processing.

    Args:
        mapping (dict): A dict representing the contents of the metadata json file.

    Returns:
        True if the entry is considered valid, False otherwise.
    """
    if not 'subject' in mapping:
        logging.warning('No subject given.')
        return False
    if 'subject' in mapping:
        try:
            int(mapping['subject'], 16)
        except Exception:
            logging.warning('Subject is not hex')
            return False
        if len(mapping['subject']) < 56 or len(mapping['subject']) % 2 != 0:
            logging.warning(
                'Subject too short (less than 28 bytes/56 chars) or has an odd number of characters. %d', len(mapping['subject']))
            return False
    if 'policy' in mapping and not mapping['policy'] is None:
        try:
            int(mapping['policy'], 16)
        except Exception:
            logging.warning('Policy is not hex')
            return False
        if len(mapping['policy']) % 2 != 0:
            logging.warning(
                'Policy has an odd number of characters. %d', len(mapping['policy']))
            return False
    if 'name' in mapping and 'value' in mapping['name'] and len(mapping['name']['value']) > 255:
        logging.warning('name property too long')
        return False
    if 'url' in mapping and 'value' in mapping['url'] and len(mapping['url']['value']) > 255:
        logging.warning('url property too long')
        return False
    if 'ticker' in mapping and 'value' in mapping['ticker'] and len(mapping['ticker']['value']) > 32:
        logging.warning('ticker property too long')
        return False

    return True


def parse_mappings_file_to_dicts(mapping: dict, source: str, author: str, updated: datetime) -> tuple:
    """Parses a json file containing metadata mappings

    Args:
        mapping (dict): the dict representing the loaded json file
        source (str): the name of the data source for the sync
        author (str): the author of the latest github commit related to that entry
        updated (datetime): the date and time when this entry was modified the last time

    Raises:
        ValueError: raised on invalid input

    Returns:
        tuple: a dict containing the json data reformatted for insertion into the target database. first element is the subjects metadata, second one the according logo if any.
    """
    metadata = []
    logo = []
    if validate_mappings_file_contents(mapping):
        subject = mapping['subject']
        metadata.append({
                        'subject': subject,
                        'source': source,
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
                    'source': source,
                    'logo': mapping['logo']['value']
                })
            else:
                logging.warning(
                    'Logo with no value field within subject %s and source %s.', subject, source)
    else:
        raise ValueError('Invalid metadata properties.')

    return metadata, logo


def populate_data(db_engine: Engine, mappings_folder_path: str, source: str):
    """ Iterate over the given mapping_folder_path and process each json file that might contain metadata information.

    Args:
        db_engine (Engine): The SQLAlchemy Engine object used to connect to the database.
        mappings_folder_path (str): The path of the folder containing the metadata json files.
    """
    try:
        db_session = Session(db_engine)

        sync_control_data = db_session.get(SyncControlDAO, 'X')
        git_commit_hash = subprocess.Popen(['git', 'rev-parse', 'HEAD'], stdout=subprocess.PIPE,
                                           stderr=subprocess.PIPE, cwd=mappings_folder_path).stdout.read().decode('utf-8').strip()
        if sync_control_data and sync_control_data.registry_hash == git_commit_hash:
            logging.info(
                'No updates since last sync. Latest commit hash is %s. Last update was %s', git_commit_hash, str(sync_control_data.updated))
            return

        truncated_current_timestamp = str(int(time.time()))[-6:]
        temp_logo_table_name = f'tmp_{truncated_current_timestamp}_logo'
        temp_metadata_table_name = f'tmp_{truncated_current_timestamp}_metadata'
        tempLogoDaoModelInstance = get_logo_table_model_instance(
            temp_logo_table_name)
        tempMetadataDaoModelInstance = get_metadata_table_model_instance(
            temp_metadata_table_name)
        logging.info('Temp table names are %s and %s',
                     temp_logo_table_name, temp_metadata_table_name)
        start_time = timeit.default_timer()
        logging.info('Preparing upload %s ...', mappings_folder_path)
        db_session.execute(
            text(f'CREATE TEMPORARY TABLE "{temp_logo_table_name}" (LIKE "logo");'))
        db_session.execute(
            text(f'CREATE TEMPORARY TABLE "{temp_metadata_table_name}" (LIKE "metadata");'))

        # iterate over each file
        metadata = []
        logos = []
        total_processed_file_size = 0
        files_processed = 0
        storage_processed = 0
        skipped_files = []
        subjects_seen = set()

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
                        if 'subject' in mappings_content:
                            if not mappings_content['subject'] in subjects_seen:
                                subjects_seen.add(mappings_content['subject'])
                                meta, logo = parse_mappings_file_to_dicts(
                                    mappings_content, source, git_metadata[0], parser.isoparse(git_metadata[1]))
                                metadata.extend(meta)
                                logos.extend(logo)

                                if total_processed_file_size >= MAX_PROCESSED_FILE_SIZE_OF_SINGLE_BATCH_BYTES:
                                    if len(metadata) > 0:
                                        db_session.bulk_insert_mappings(
                                            tempMetadataDaoModelInstance, metadata)
                                    if len(logos) > 0:
                                        db_session.bulk_insert_mappings(
                                            tempLogoDaoModelInstance, logos)
                                    total_processed_file_size = 0
                                    metadata = []
                                    logos = []
                            else:
                                logging.warning('Duplicate subject (%s) detected within file (%s)', mappings_content['subject'], file)
                                skipped_files.append(file)
                        else:
                            logging.warning('Skipping file because there is no subject property. %s', file)
                            skipped_files.append(file)
            except ValueError as exc:
                logging.warning(
                    'Invalid metadata mappings file %s. Will be skipped.', file)
                logging.exception(exc)
                if mappings_content:
                    logging.debug(mappings_content)
                skipped_files.append(file)
            except Exception as exc:
                logging.warning(
                    'Error during parsing of file %s. Will be skipped.', file)
                logging.exception(exc)
        if len(metadata) > 0:
            db_session.bulk_insert_mappings(
                tempMetadataDaoModelInstance, metadata)
        if len(logos) > 0:
            db_session.bulk_insert_mappings(tempLogoDaoModelInstance, logos)

        db_session.commit()
        logging.info(
            'Duration of data preparation was %s seconds', "{:.2f}".format(timeit.default_timer() - start_time))

        start_time = timeit.default_timer()
        logging.info('Truncating existing data ...')
        db_session.execute(text(f'DELETE from "logo" where "source" = \'{source}\';'))
        db_session.execute(text(f'DELETE from "metadata" where "source" = \'{source}\';'))

        logging.info(
            'Inserting new data from folder %s ...', mappings_folder_path)
        db_session.execute(
            text(f'INSERT INTO "metadata"("subject", "source", "policy", "name", "ticker", "url", "description", "decimals", "updated", "updated_by", "properties") SELECT "subject", "source", "policy", "name", "ticker", "url", "description", "decimals", "updated", "updated_by", "properties" FROM "{temp_metadata_table_name}";'))
        db_session.execute(
            text(f'INSERT INTO "logo" SELECT * FROM "{temp_logo_table_name}";'))

        db_session.commit()

        if sync_control_data:
            sync_control_data.registry_hash = git_commit_hash
            sync_control_data.updated = datetime.utcnow()
            db_session.commit()
        else:
            sync_control_data = SyncControlDAO()
            sync_control_data.lock = 'X'
            sync_control_data.registry_hash = git_commit_hash
            sync_control_data.updated = datetime.utcnow()
            db_session.add(sync_control_data)
            db_session.commit()

        db_session.close()
        logging.info('Duration of data recreation was %s seconds',
                     "{:.2f}".format(timeit.default_timer() - start_time))
        logging.info('Done processing %s files containing %s MB of data. Skipped %d files.',
                     files_processed, "{:.2f}".format(storage_processed/1024/1024), len(skipped_files))
        logging.debug('Skipped files: %s', str(skipped_files))
    except Exception as exc:
        logging.error('Could not insert metadata into database.')
        logging.exception(exc)
        if db_session:
            db_session.close()


if __name__ == '__main__':
    db_engine = create_db_engine(args.dbuser, args.dbsecret, args.dburl)

    logging.info('Source specified is %s', args.source)
    populate_data(db_engine, args.mappings, args.source)

    db_engine.dispose()
