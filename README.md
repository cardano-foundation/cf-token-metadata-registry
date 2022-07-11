[![License](https://img.shields.io/github/license/cardano-foundation/cf-metadata-server)](https://github.com/cardano-foundation/cf-metadata-server/blob/main/LICENSE)
![GitHub top language](https://img.shields.io/github/languages/top/cardano-foundation/cf-metadata-server)
![coverage](https://github.com/cardano-foundation/cf-metadata-server/blob/badges/jacoco.svg)
![branches](https://github.com/cardano-foundation/cf-metadata-server/blob/badges/branches.svg)
[![Issues](https://img.shields.io/github/issues/cardano-foundation/cf-metadata-server)](https://github.com/cardano-foundation/cf-metadata-server/issues)

# Cardano offchain metadata registry
A reference implementation of a Cardano [CIP-26](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0026) compliant offchain metadata registry.

## Introduction

This repository contains an implementaiton of a [CIP-26](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0026) compliant offchain metadata registry for Cardano. It exposes an extended API (see the OpenAPI v3 spec hosted by our staging deployments [here](https://api.metadata.staging.cf-deployments.org/apidocs)) with regards to the one specified in [CIP-26](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0026) to allow for more querying options and also write access to the registry to get rid of parts of the GitHub based process (but still stick to the known [GitHub repository](https://github.com/cardano-foundation/cardano-token-registry) as a single source of truth for now).

Implementation of client tools for metadata creation can be found [here](https://github.com/cardano-foundation/cf-metadata-app) and at [IOG's reference implementation repository](https://github.com/input-output-hk/offchain-metadata-tools).

## Getting started

### Prerequisites
At the moment the application needs a Postgres database as a storage layer. This will change in the future. However, you can use the [liquibase](https://www.liquibase.org/) database migration scripts provided in our [deployment repository](https://github.com/cardano-foundation/cf-metadata-deployment/tree/main/database/liquibase) to initialize the database.

If you are not familiar with PostgreSQL here are the few steps needed to spin up an instance using Docker and use some of our deployment scripts to populate the correct schema, create a user which can be used by the REST service.

REMARK: In order to use the populate_data script you need [Python3](https://www.python.org/downloads/).

```sh
# spin up a dockerized postgres instance, set the admin credentials and default database name
docker run -p 5432:5432 -e POSTGRES_PASSWORD=n0tr3allyS3cuR3 -e POSTGRES_USER=cardano_admin -d postgres

# clone the deployment repository
git clone git@github.com:cardano-foundation/cf-metadata-deployment.git

# enter the database migration folder within the deployment repository
pushd cf-metadata-deployment/database/scripts

# rollout the schema and create a user that can later be used by the REST API service
DBA_USER_SECRET=n0tr3allyS3cuR3 DBA_USER_NAME=cardano_admin METADATA_DB_NAME=cf_metadata SERVICE_USER_SECRET=again3asy2hack SERVICE_USER_NAME=cardano_service ./bootstrap_database.sh
DBA_USER_SECRET=n0tr3allyS3cuR3 DBA_USER_NAME=cardano_admin METADATA_DB_NAME=cf_metadata SERVICE_USER_SECRET=again3asy2hack SERVICE_USER_NAME=cardano_service ./migrate_database.sh

# populate some data into the database using our own deployment scripts hosted at https://github.com/cardano-foundation/cf-metadata-deployment
popd
mkdir tmp
pushd tmp
git clone git@github.com:cardano-foundation/cardano-token-registry.git
pushd cardano-token-registry
git checkout master
git pull
export REGISTRY_CLONE_FOLDER="`pwd`/"
popd
popd
pushd cf-metadata-deployment/deployment/aws/images/gitsync-task
DBA_USER_SECRET=n0tr3allyS3cuR3 DBA_USER_NAME=cardano_admin METADATA_DB_NAME=cf_metadata SERVICE_USER_SECRET=again3asy2hack SERVICE_USER_NAME=cardano_service ./populate_data.sh
popd
rm -Rf tmp
```

### How to build?

Clone this repo
```sh
git clone git@github.com:cardano-foundation/cf-metadata-server.git
```

`cd` into the directory where `git` did clone the sources into and build the application via Maven
```sh
cd cf-metadata-server
mvn package
```

Run the application (assuming we are using the database setup in the section above)
```sh
java -DdbUser="cardano_service" -DdbSecret="again3asy2hack" -jar api/target/api-1.0.0-SNAPSHOT.jar
```

Check if the application is running and connectivity to the database is given
```sh
curl http://localhost:8080/v2/health
```

### How to run?
There exists a [shell script](./api/run_locally.sh) in this repository that can be used to spin up the application including a dockerized PostgreSQL instance, have the latest schema deployed, the latest data from the [registry GitHub](https://github.com/cardano-foundation/cardano-token-registry) populated and the application initialized to run on port 8081 listening for http connections.
```sh
./run_local.sh -b
```

Following parameters can be used to configure the application:
| Parameter name | Description | Default |
| ----------- | ----------- | ----------- |
| dbConnectionParamsProviderType | Specify whether database connection config shall be pulled from system properties or AWS SSM. Possible values are `ENVIRONMENT` and `AWS_SSM` | `ENVIRONMENT` |
| dbUser | user name used to access the database | `none` |
| dbSecret | password used to access the database | `none` |
| dbDriverName | the database driver to use | `org.postgresql.Driver` |
| dbUrl | connection string without user credentials | `jdbc:postgresql://localhost:5432/cf_metadata` |
| region | the AWS region the AWS SSM parameters are deployed to | `none` |
| rdsUsernameSsmParameterName | the name of the AWS SSM parameter holding the database user name | `none` |
| rdsPasswordSsmParameterName | the name of the AWS SSM parameter holding the database password | `none` |
| rdsUrlSsmParameterName | the name of the AWS SSM parameter holding the database connection string | `none` |
| rdsDriverClassNameSsmParameterName | the name of the AWS SSM parameter holding the database driver name | `none` |

## Features

Offchain metadata related:
- [x] Expose CIP-26 compliant REST API
- [x] Advanced querrying REST API

WIP:
- [ ] Expose write access functions via REST API for creating, modifying and deleting offchain metadata
- [ ] Expose verification API for offchain metadata based on extended CIP-26 trust concept

Later:
- [ ] Modular storage backend with support for different RDS backends (SQLite, MySQL), S3 based storage (MinIO, AWS), MongoDB
- [ ] JSON-LD support and schema.org registration
- [ ] Include onchain token metadata like proposed in a [CIP draft](https://github.com/cardano-foundation/CIPs/pull/137)
- [ ] Example deployments using Kubernetes and Docker Compose
- [ ] Decentralized offchain registry based on QUIC and Gossip

## Contributing

File an issue or a PR or reach out directly to us if you want to contribute.

When contributing to this project and interacting with others, please follow our [Contributing Guidelines](./CONTRIBUTING.md) and [Code of Conduct](./CODE-OF-CONDUCT.md).

---

Thanks for visiting and enjoy :heart:!
