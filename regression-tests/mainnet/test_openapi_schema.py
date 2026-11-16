"""
The OpenAPI spec of the instance (/apidocs) describes what it really sends.

Each response is validated against the schema the spec publishes for it, as a client generated from the spec, or a strict
validator, would read it (OpenAPI 3.1, JSON Schema 2020-12). It covers what the API writes as null (a property a token
does not have, standards without show_cips_details), the extensions of CIP-113 tokens and the validation errors.
"""

import allure
import pytest
import requests
from jsonschema import Draft202012Validator
from referencing import Registry, Resource
from referencing.jsonschema import DRAFT202012

from .conftest import _cip26_sample, _cip68_sample, _load_json, API_BASE_URL

RFT_SUBJECTS = [t["subject"] for t in _load_json("cip68_rft_tokens.json")["tokens"]][::25]
CIP113_SUBJECTS = [s["subject"] for p in _load_json("cip113_tokens.json")["policies"] for s in p["subjects"]]
VALIDATION_ERROR_SUBJECTS = [t["subject"] for t in _load_json("cip68_validation_errors_tokens.json")]
V2_SUBJECTS = _cip26_sample(30) + _cip68_sample(30) + RFT_SUBJECTS + CIP113_SUBJECTS + VALIDATION_ERROR_SUBJECTS


@pytest.fixture(scope="session")
def validator_for():
    spec = requests.get(f"{API_BASE_URL}/apidocs", timeout=30).json()
    registry = Registry().with_resource("urn:apidocs", Resource(spec, specification=DRAFT202012))

    def validator(schema_name):
        return Draft202012Validator({"$ref": f"urn:apidocs#/components/schemas/{schema_name}"}, registry=registry)

    return validator


def assert_valid(validator, body):
    errors = ["/".join(map(str, e.absolute_path)) + ": " + e.message[:200] for e in validator.iter_errors(body)]
    assert not errors, "\n".join(errors[:10])


@allure.epic("OpenAPI")
@allure.feature("Responses match /apidocs")
@pytest.mark.openapi
class TestOpenApiSchema:

    @allure.story("V2 GET subject matches the Response schema")
    @pytest.mark.parametrize("show_cips_details", [False, True], ids=["plain", "show_cips_details"])
    @pytest.mark.parametrize("subject", V2_SUBJECTS, ids=lambda s: s[:16])
    def test_v2_get_subject(self, validator_for, subject, show_cips_details):
        resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{subject}",
                            params={"show_cips_details": str(show_cips_details).lower()}, timeout=30)
        assert resp.status_code == 200
        assert_valid(validator_for("Response"), resp.json())

    @allure.story("V1 GET subject matches the TokenMetadata schema")
    @pytest.mark.parametrize("subject", _cip26_sample(20), ids=lambda s: s[:16])
    def test_v1_get_subject(self, validator_for, subject):
        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}", timeout=30)
        assert resp.status_code == 200
        assert_valid(validator_for("TokenMetadata"), resp.json())

    @allure.story("V1 batch query matches the BatchResponse schema")
    def test_v1_batch_query(self, validator_for):
        resp = requests.post(f"{API_BASE_URL}/metadata/query", json={"subjects": _cip26_sample(20)}, timeout=60)
        assert resp.status_code == 200
        assert_valid(validator_for("BatchResponse"), resp.json())
