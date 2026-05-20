"""
V1 API: POST /metadata/query

Tests batch query endpoint for CIP-26 tokens.
"""

import allure
import pytest
import requests

from .conftest import _cip26_token_ids, _load_json, API_BASE_URL

CIP26_SUBJECTS = _cip26_token_ids()
CIP26_BY_SUBJECT = {t["subject"]: t for t in _load_json("cip26_tokens.json")}


@allure.epic("V1 API")
@allure.feature("POST /metadata/query")
@pytest.mark.v1
@pytest.mark.batch
@pytest.mark.cip26
class TestV1BatchQuery:

    @allure.story("Batch query returns all requested subjects")
    def test_batch_all_subjects(self):
        """Query all CIP-26 subjects in batches of 50 and verify each is returned."""
        batch_size = 50
        for i in range(0, len(CIP26_SUBJECTS), batch_size):
            batch = CIP26_SUBJECTS[i:i + batch_size]
            resp = requests.post(
                f"{API_BASE_URL}/metadata/query",
                json={"subjects": batch},
            )
            assert resp.status_code == 200
            data = resp.json()
            returned_subjects = {s["subject"] for s in data["subjects"]}

            for subj in batch:
                assert subj in returned_subjects, f"Subject {subj[:16]}... not in batch response"

    @allure.story("Batch query with properties filter returns only requested properties")
    def test_batch_with_properties_filter(self):
        batch = CIP26_SUBJECTS[:20]
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": batch, "properties": ["name", "ticker"]},
        )
        assert resp.status_code == 200
        data = resp.json()

        for entry in data["subjects"]:
            if entry.get("name"):
                assert "value" in entry["name"]
            # Unfiltered properties should be absent
            assert entry.get("description") is None
            assert entry.get("url") is None
            assert entry.get("decimals") is None
            assert entry.get("logo") is None

    @allure.story("Batch query values match DB fixture")
    def test_batch_values_match_fixture(self):
        batch = CIP26_SUBJECTS[:50]
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": batch},
        )
        assert resp.status_code == 200
        data = resp.json()

        for entry in data["subjects"]:
            subject = entry["subject"]
            expected = CIP26_BY_SUBJECT[subject]

            if expected["name"] is not None and entry.get("name"):
                assert entry["name"]["value"] == expected["name"]
            if expected["ticker"] is not None and entry.get("ticker"):
                assert entry["ticker"]["value"] == expected["ticker"]
            if expected["description"] is not None and entry.get("description"):
                assert entry["description"]["value"] == expected["description"]
            if expected["decimals"] is not None and entry.get("decimals"):
                assert entry["decimals"]["value"] == expected["decimals"]

    @allure.story("Batch query with mixed known and unknown subjects")
    def test_batch_mixed_known_unknown(self):
        known = CIP26_SUBJECTS[:5]
        unknown = ["00" * 56, "ff" * 56]
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": known + unknown},
        )
        assert resp.status_code == 200
        data = resp.json()
        returned_subjects = {s["subject"] for s in data["subjects"]}

        for subj in known:
            assert subj in returned_subjects
        for subj in unknown:
            assert subj not in returned_subjects

    @allure.story("Batch query with empty subjects returns 200 with empty result")
    def test_batch_empty_subjects(self):
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": []},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["subjects"] == []

    @allure.story("Batch query with no body returns 400")
    def test_batch_no_body(self):
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            headers={"Content-Type": "application/json"},
        )
        assert resp.status_code == 400

    @allure.story("Batch query returns policy for all subjects")
    def test_batch_returns_policy(self):
        batch = CIP26_SUBJECTS[:20]
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": batch},
        )
        assert resp.status_code == 200
        data = resp.json()
        for entry in data["subjects"]:
            expected = CIP26_BY_SUBJECT[entry["subject"]]
            if expected["policy"]:
                assert entry.get("policy") == expected["policy"]

    @allure.story("Large batch query (all tokens)")
    def test_large_batch(self):
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": CIP26_SUBJECTS},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["subjects"]) == len(CIP26_SUBJECTS)

    @allure.story("Batch response preserves V1 annotated-property shape")
    def test_batch_response_contains_annotated_shape(self):
        """Each property in a batch response must be the CIP-26 annotated object:
        {value, signatures, sequenceNumber}. Regression guard for V1 mapper paths."""
        batch = CIP26_SUBJECTS[:20]
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": batch},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["subjects"], "Expected at least one subject in batch response"

        annotated_props = ["name", "description", "ticker", "url", "decimals", "logo"]
        checked_any_property = False
        for entry in data["subjects"]:
            for prop in annotated_props:
                prop_data = entry.get(prop)
                if prop_data is None:
                    continue
                checked_any_property = True
                assert "value" in prop_data, f"{entry['subject'][:16]}.{prop} missing 'value'"
                assert "signatures" in prop_data, f"{entry['subject'][:16]}.{prop} missing 'signatures'"
                assert "sequenceNumber" in prop_data, f"{entry['subject'][:16]}.{prop} missing 'sequenceNumber'"
                assert isinstance(prop_data["signatures"], list), (
                    f"{entry['subject'][:16]}.{prop}.signatures must be a list"
                )
                assert isinstance(prop_data["sequenceNumber"], (int, float)), (
                    f"{entry['subject'][:16]}.{prop}.sequenceNumber must be numeric"
                )
        assert checked_any_property, "Did not find any annotated property to validate"

    @allure.story("Batch with property filter preserves annotated shape on filtered fields")
    def test_batch_filter_preserves_annotated_shape(self):
        batch = CIP26_SUBJECTS[:20]
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": batch, "properties": ["name", "description"]},
        )
        assert resp.status_code == 200
        data = resp.json()
        for entry in data["subjects"]:
            for prop in ("name", "description"):
                prop_data = entry.get(prop)
                if prop_data is None:
                    continue
                assert "value" in prop_data
                assert "signatures" in prop_data
                assert "sequenceNumber" in prop_data
