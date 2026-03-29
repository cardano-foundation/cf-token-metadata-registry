"""
V1 API: GET /metadata/{subject}/properties/{property}

Tests that individual property endpoints return the correct values.
"""

import allure
import pytest
import requests

from conftest import _cip26_sample, _load_json, API_BASE_URL

CIP26_SAMPLE = _cip26_sample(50)
CIP26_BY_SUBJECT = {t["subject"]: t for t in _load_json("cip26_tokens.json")}

PROPERTIES = ["name", "ticker", "description", "url", "decimals", "logo"]


@allure.epic("V1 API")
@allure.feature("GET /metadata/{subject}/properties/{property}")
@pytest.mark.v1
@pytest.mark.cip26
class TestV1GetProperty:

    @allure.story("Return specific property for known subject")
    @pytest.mark.parametrize("subject", CIP26_SAMPLE, ids=lambda s: s[:16])
    @pytest.mark.parametrize("prop", ["name", "description"])
    def test_get_required_property(self, subject, prop):
        expected = CIP26_BY_SUBJECT[subject]
        expected_val = expected.get(prop)

        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}/properties/{prop}")

        if expected_val is None:
            assert resp.status_code in (200, 204)
            return

        assert resp.status_code == 200
        data = resp.json()
        assert data["subject"] == subject

        prop_data = data.get(prop)
        assert prop_data is not None, f"Expected {prop} in response"
        assert prop_data["value"] == expected_val

    @allure.story("Return optional property (ticker, url, decimals)")
    @pytest.mark.parametrize("subject", CIP26_SAMPLE[:20], ids=lambda s: s[:16])
    @pytest.mark.parametrize("prop", ["ticker", "url", "decimals"])
    def test_get_optional_property(self, subject, prop):
        expected = CIP26_BY_SUBJECT[subject]
        expected_val = expected.get(prop)

        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}/properties/{prop}")

        if expected_val is None:
            assert resp.status_code in (200, 204)
            return

        assert resp.status_code == 200
        data = resp.json()
        assert data["subject"] == subject

        prop_data = data.get(prop)
        if prop_data is not None:
            assert prop_data["value"] == expected_val

    @allure.story("Return logo property")
    @pytest.mark.parametrize("subject", CIP26_SAMPLE[:10], ids=lambda s: s[:16])
    def test_get_logo_property(self, subject):
        expected = CIP26_BY_SUBJECT[subject]
        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}/properties/logo")

        if not expected.get("has_logo"):
            assert resp.status_code in (200, 204)
            return

        assert resp.status_code == 200
        data = resp.json()
        logo = data.get("logo")
        assert logo is not None, "Expected logo in response"
        assert logo["value"] is not None
        assert len(logo["value"]) > 0

    @allure.story("Return 204 for unknown subject property lookup")
    def test_get_property_unknown_subject(self):
        resp = requests.get(f"{API_BASE_URL}/metadata/{'00' * 56}/properties/name")
        assert resp.status_code == 204

    @allure.story("Only requested property is present in filtered response")
    def test_only_requested_property_returned(self):
        subject = CIP26_SAMPLE[0]
        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}/properties/name")
        assert resp.status_code == 200
        data = resp.json()
        assert data.get("name") is not None
        # Other properties should not be present (or null)
        for other in ["ticker", "description", "url", "decimals", "logo"]:
            val = data.get(other)
            assert val is None, f"Property '{other}' should not be present when requesting only 'name'"
