"""
CIP-26 entries whose registry file writes the subject in upper case hex (39 on mainnet), on V1 (GET /metadata/{subject},
POST /metadata/query) and V2 (GET /api/v2/subjects/{subject}, POST /api/v2/subjects/query, standards.cip26).

The entry is stored and looked up in lower case, so a client finds it whichever case it asks in. The CIP-26 entry served
must carry the subject as the file writes it: every signature is made over that string, and none verifies against the
subject in lower case. Three entries from cip26_upper_case_subjects.json (cardano-token-registry 5503caec), with their
real signatures.
"""

import allure
import pytest
import requests

from shared.cip26_signatures import verify_entry

from .conftest import _load_json, API_BASE_URL

ENTRIES = _load_json("cip26_upper_case_subjects.json")
CASES = ["lower", "as_written"]


def _ids(entry):
    return entry["name"]


def _asked(entry, case):
    return entry["subject"].lower() if case == "lower" else entry["subject"]


def _assert_served_as_written(cip26, entry):
    assert cip26 is not None, f"no CIP-26 entry for {entry['name']}"
    assert cip26["subject"] == entry["subject"], (
        f"{entry['name']}: the subject served is {cip26['subject']}, the registry file writes {entry['subject']}")
    checked, failed = verify_entry(cip26)
    assert not failed, f"{entry['name']}: signatures that do not verify against the subject served: {failed}"
    assert checked >= len(entry["signed_properties"]), (
        f"{entry['name']}: {checked} signatures served, the file signs {entry['signed_properties']}")
    _, failed_in_lower_case = verify_entry(cip26, subject=entry["subject"].lower())
    assert failed_in_lower_case, f"{entry['name']}: a signature verified against the lower-case subject too"


@allure.epic("V1 API")
@allure.feature("CIP-26 signatures of subjects written in upper case")
@pytest.mark.v1
@pytest.mark.cip26
@pytest.mark.signatures
class TestV1Cip26UpperCaseSubjects:

    @allure.story("Found in either case, served with the subject as written, every signature verifies")
    @pytest.mark.parametrize("entry", ENTRIES, ids=_ids)
    @pytest.mark.parametrize("case", CASES)
    def test_get_subject(self, entry, case):
        resp = requests.get(f"{API_BASE_URL}/metadata/{_asked(entry, case)}")
        assert resp.status_code == 200, f"{entry['name']} asked {case}: got {resp.status_code}"
        _assert_served_as_written(resp.json(), entry)

    @allure.story("A batch asked in either case: every entry served with the subject as written")
    @pytest.mark.parametrize("case", CASES)
    def test_batch(self, case):
        resp = requests.post(f"{API_BASE_URL}/metadata/query", json={"subjects": [_asked(e, case) for e in ENTRIES]})
        assert resp.status_code == 200

        served = {s["subject"]: s for s in resp.json().get("subjects", [])}
        for entry in ENTRIES:
            assert entry["subject"] in served, f"{entry['name']} asked {case}: not in the batch with the subject as written"
            _assert_served_as_written(served[entry["subject"]], entry)


@allure.epic("V2 API")
@allure.feature("CIP-26 signatures of subjects written in upper case")
@pytest.mark.v2
@pytest.mark.cip26
@pytest.mark.signatures
class TestV2Cip26UpperCaseSubjects:

    CIP26_DETAILS = {"query_priority": "CIP_26", "show_cips_details": "true"}

    @allure.story("Found in either case, standards.cip26 has the subject as written, every signature verifies")
    @pytest.mark.parametrize("entry", ENTRIES, ids=_ids)
    @pytest.mark.parametrize("case", CASES)
    def test_get_subject(self, entry, case):
        resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{_asked(entry, case)}", params=self.CIP26_DETAILS)
        assert resp.status_code == 200, f"{entry['name']} asked {case}: got {resp.status_code}"

        subject = resp.json()["subject"]
        assert subject["metadata"]["name"]["value"] == entry["name"]
        _assert_served_as_written(subject["standards"]["cip26"], entry)

    @allure.story("A batch asked in either case: every entry served with the subject as written")
    @pytest.mark.parametrize("case", CASES)
    def test_batch(self, case):
        resp = requests.post(f"{API_BASE_URL}/api/v2/subjects/query", params=self.CIP26_DETAILS,
                             json={"subjects": [_asked(e, case) for e in ENTRIES]})
        assert resp.status_code == 200

        served = {s["standards"]["cip26"]["subject"]: s for s in resp.json()["subjects"] if (s.get("standards") or {}).get("cip26")}
        for entry in ENTRIES:
            assert entry["subject"] in served, f"{entry['name']} asked {case}: not in the batch with the subject as written"
            _assert_served_as_written(served[entry["subject"]]["standards"]["cip26"], entry)
