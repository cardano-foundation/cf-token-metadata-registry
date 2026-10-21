"""
V2 API: the validation contract of ADR-019, on samples of the CIP-26 and CIP-68 baselines.

Every token is served with a `validation_errors` array (empty when it follows its CIP), `metadata.valid` is true exactly
when it is empty, `?validation=MANDATORY_ONLY|STRICT` returns a token exactly when its errors allow it, and under
`?property=` the errors are about the requested properties only. What each token's errors are is not asserted here
(see test_v2_cip68_validation_errors.py): only that the response is consistent with them.
"""

import allure
import pytest
import requests

from .conftest import _cip26_sample, _cip68_sample, API_BASE_URL

SAMPLE = _cip26_sample(25) + _cip68_sample(25)


def get(subject, **params):
    return requests.get(f"{API_BASE_URL}/api/v2/subjects/{subject}", params=params)


def has_mandatory(errors):
    return any(e["field_category"] == "MANDATORY" for e in errors)


@allure.epic("V2 API")
@allure.feature("Validation errors (ADR-019)")
@pytest.mark.v2
@pytest.mark.validation
class TestV2Validation:

    @allure.story("Every token has a validation_errors array, and metadata.valid says whether it is empty")
    @pytest.mark.parametrize("subject", SAMPLE, ids=lambda s: s[:16])
    def test_errors_and_valid_agree(self, subject):
        resp = get(subject)
        assert resp.status_code == 200
        body = resp.json()["subject"]
        errors = body["validation_errors"]
        assert isinstance(errors, list)
        assert body["metadata"]["valid"] is (len(errors) == 0)
        for error in errors:
            assert error["cip"] in ("CIP-26", "CIP-68")
            assert error["field_category"] in ("MANDATORY", "OPTIONAL")
            assert error["code"] and error["error"] and error["field_name"]

    @allure.story("?validation= returns a token exactly when its errors allow it")
    @pytest.mark.parametrize("subject", SAMPLE, ids=lambda s: s[:16])
    def test_validation_scope(self, subject):
        errors = get(subject).json()["subject"]["validation_errors"]

        assert get(subject, validation="ALL").status_code == 200
        assert get(subject, validation="MANDATORY_ONLY").status_code == (404 if has_mandatory(errors) else 200)
        assert get(subject, validation="STRICT").status_code == (404 if errors else 200)

    @allure.story("Under ?property= the errors are about the requested properties only")
    @pytest.mark.parametrize("subject", SAMPLE[::5], ids=lambda s: s[:16])
    def test_errors_scoped_to_properties(self, subject):
        resp = get(subject, property="ticker")
        assert resp.status_code == 200
        for error in resp.json()["subject"]["validation_errors"]:
            assert error["field_name"] in ("ticker", "721"), error

    @allure.story("A STRICT batch returns exactly the tokens with no error")
    def test_strict_batch(self):
        all_tokens = requests.post(f"{API_BASE_URL}/api/v2/subjects/query", json={"subjects": SAMPLE}).json()["subjects"]
        strict = requests.post(f"{API_BASE_URL}/api/v2/subjects/query", params={"validation": "STRICT"},
                               json={"subjects": SAMPLE}).json()["subjects"]

        assert {s["subject"] for s in strict} == {s["subject"] for s in all_tokens if not s["validation_errors"]}
