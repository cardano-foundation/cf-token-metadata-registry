[![License](https://img.shields.io/github/license/cardano-foundation/cf-token-metadata-registry)](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/main/LICENSE)
![GitHub top language](https://img.shields.io/github/languages/top/cardano-foundation/cf-token-metadata-registry)
[![Build](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/main.yaml/badge.svg)](https://github.com/cardano-foundation/cf-token-metadata-registry/actions/workflows/main.yaml)
![coverage](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/badges/jacoco.svg)
![branches](https://github.com/cardano-foundation/cf-token-metadata-registry/blob/badges/branches.svg)
[![Issues](https://img.shields.io/github/issues/cardano-foundation/cf-token-metadata-registry)](https://github.com/cardano-foundation/cf-token-metadata-registry/issues)

---

# Cardano offchain metadata registry

A reference implementation of a Cardano [CIP-26](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0026) compliant offchain metadata registry.

## Introduction

This repository contains an implementaiton of a [CIP-26](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0026) compliant offchain metadata registry for Cardano. It exposes an extended API (see the OpenAPI v3 spec hosted by our staging deployments [here](https://api.metadata.staging.cf-deployments.org/apidocs)) with regards to the one specified in [CIP-26](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0026) to allow for more querying options and also write access to the registry to get rid of parts of the GitHub based process (but still stick to the known [GitHub repository](https://github.com/cardano-foundation/cardano-token-registry) as a single source of truth for now).

Implementation of client tools for metadata creation can be found [here](https://github.com/cardano-foundation/cf-metadata-app) and at [IOG's reference implementation repository](https://github.com/input-output-hk/offchain-metadata-tools).

## How to build?
For building you need
- [Apache Maven](https://maven.apache.org/)
- [Java SDK](https://adoptium.net/installation/)
- [Git](https://git-scm.com/)

### Building from source
Clone this repository
```console
$ git clone git@github.com:cardano-foundation/cf-token-metadata-registry.git
```

`cd` into the directory where `git` did clone the sources into and build the application via Maven
```console
$ cd cf-token-metadata-registry
$ mvn package
```

## How to run?

The simplest approach to running the application after having built it is to use the provided [Docker Compose](./docker-compose.yml) setup and Docker containers included in the various sub folders. This is done simply by calling the following command in the root directory of this repository:
```console
$ docker compose up
```

> [!NOTE]
> If you change the code, in order to make the changes visible to docker compose you need to rebuild the local image 
> with `docker compose build` and then `docker compose up` will take care to restart containers who have a new image available

The complete Docker Compose setup runs the following services:
1. Setup a Postgres database in a docker container and exposes it to `localhost` on port `5432` (service name `db`)
2. Starts the actual Spring application that exposes the CIP-26 REST API and exposes it to `localhost` on port `8081` (service name `api`)

To test if the API is running query its health endpoint by executing:
```console
$ curl http://localhost:8081/actuator/health
```

Have a look at the [.env file](./.env) for the various configuration options.

At the moment the application needs a PostgreSQL database as a storage layer which might change in the future. Database evolutions
are managed by the `api` project using [flyway](https://flywaydb.org/).

## Features

Offchain metadata related:
- [x] Expose CIP-26 compliant REST API
- [x] Advanced querrying REST API

WIP:
- [ ] Expose write access functions via REST API for creating, modifying and deleting offchain metadata
- [ ] Expose verification API for offchain metadata based on extended CIP-26 trust concept
- [ ] Provide a CLI application for metadata creation and verification
- [ ] Implement metadata verification based on public key registries
- [ ] Implement metadata verification based on SSI trust registries

## Contributing

File an issue or a PR or reach out directly to us if you want to contribute.

When contributing to this project and interacting with others, please follow our [Contributing Guidelines](./CONTRIBUTING.md) and [Code of Conduct](./CODE-OF-CONDUCT.md).

---

Thanks for visiting and enjoy :heart:!
