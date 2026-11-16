#!/usr/bin/env bash
# The pytest markers each test step selects. Every regression test must be selected by one of them:
# run-cip26-tests.sh fails on a test that neither selects, so none is skipped silently.
#   CIP-26 step, once the offchain sync is up:  CIP-26 data only
#   CIP-68 step, at the chain tip:              everything that needs on-chain data (CIP-68, the validation
#                                               contract, CIP-113) or every standard indexed (the OpenAPI checks)
# shellcheck disable=SC2034
CIP26_MARKERS="cip26"
CIP68_MARKERS="cip68 or validation or cip113 or openapi"
