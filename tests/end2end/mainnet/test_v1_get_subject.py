"""
V1 API: GET /metadata/{subject}

Tests that each CIP-26 token returns correct metadata matching the DB fixture.
"""

import allure
import pytest
import requests

from .conftest import _cip26_token_ids, _load_json, API_BASE_URL

CIP26_SUBJECTS = _cip26_token_ids()
CIP26_BY_SUBJECT = {t["subject"]: t for t in _load_json("cip26_tokens.json")}


@allure.epic("V1 API")
@allure.feature("GET /metadata/{subject}")
@pytest.mark.v1
@pytest.mark.cip26
class TestV1GetSubject:

    @allure.story("Return full metadata for known CIP-26 subject")
    @pytest.mark.parametrize("subject", CIP26_SUBJECTS, ids=lambda s: s[:16])
    def test_get_subject_returns_200(self, subject):
        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}")
        assert resp.status_code == 200, f"Expected 200 for {subject}, got {resp.status_code}"

        data = resp.json()
        expected = CIP26_BY_SUBJECT[subject]

        assert data["subject"] == subject
        self._assert_field(data, expected, "name")
        self._assert_field(data, expected, "ticker")
        self._assert_field(data, expected, "description")
        self._assert_field(data, expected, "url")
        self._assert_decimals(data, expected)

        if expected["policy"]:
            assert data.get("policy") == expected["policy"]

    @allure.story("Return 204 for unknown subject")
    def test_get_unknown_subject_returns_204(self):
        resp = requests.get(f"{API_BASE_URL}/metadata/{'00' * 56}")
        assert resp.status_code == 204, f"Expected 204 for unknown subject, got {resp.status_code}"

    @allure.story("V1 response contains signatures for CIP-26 properties")
    def test_response_contains_signatures(self):
        subject = CIP26_SUBJECTS[0]
        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}")
        data = resp.json()

        if data.get("name"):
            assert "signatures" in data["name"], "name property should contain signatures"
            assert "sequenceNumber" in data["name"], "name property should contain sequenceNumber"
            assert "value" in data["name"], "name property should contain value"

    @allure.story("V1 response includes additionalProperties map")
    def test_response_has_additional_properties(self):
        subject = CIP26_SUBJECTS[0]
        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}")
        data = resp.json()
        assert "additionalProperties" in data

    def _assert_field(self, data, expected, field):
        expected_val = expected.get(field)
        if expected_val is not None:
            actual = data.get(field)
            assert actual is not None, f"Expected {field} to be present"
            assert actual["value"] == expected_val, (
                f"{field}: expected '{expected_val}', got '{actual['value']}'"
            )

    def _assert_decimals(self, data, expected):
        expected_val = expected.get("decimals")
        if expected_val is not None:
            actual = data.get("decimals")
            assert actual is not None, "Expected decimals to be present"
            assert actual["value"] == expected_val, (
                f"decimals: expected {expected_val}, got {actual['value']}"
            )
