"""
V2 API: CIP-113 programmable tokens, against a committed fixture of the network's CIP-113 registry.

The fixture (<network>/fixtures/cip113_tokens.json, made by shared/generate_cip113_fixtures.py from the chain) holds,
for every policy registered in the official CIP-113 registry, the extension the API must return and the tokens minted
under that policy. A programmable token is served only when it also has valid CIP-26 or CIP-68 metadata: CIP-113 adds
an extension to a token, it does not describe one. So for each subject:

- metadata valid by the registry's rules: 200, type PROGRAMMABLE, and extensions.cip113 equal to the registry node;
- no metadata, or metadata the registry rejects (listed in the network's NOT_SERVED with the reason): 404.

Each network's test module builds its test class with make_test_class.
"""

import json
import os

import allure
import pytest
import requests

API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
CIP113_FIELDS = ("minting_logic_script", "transfer_logic_script", "third_party_transfer_logic_script",
                 "unfracking_logic_script", "global_state_policy_id")


def load_fixture(test_module_file):
    path = os.path.join(os.path.dirname(os.path.abspath(test_module_file)), "fixtures", "cip113_tokens.json")
    with open(path) as f:
        return json.load(f)


def _cases(fixture):
    return [(s, p) for p in fixture["policies"] for s in p["subjects"]]


def _has_metadata(subject):
    return subject["koios_cip26"] or bool(subject["koios_cip68_labels"])


def _assert_programmable(entry, policy):
    assert entry["type"] == "PROGRAMMABLE", f"{entry['subject'][:24]}...: type {entry.get('type')}"
    cip113 = (entry.get("extensions") or {}).get("cip113")
    assert cip113 is not None, f"{entry['subject'][:24]}...: no extensions.cip113"
    for field in CIP113_FIELDS:
        assert cip113.get(field) == policy["cip113"][field], (
            f"{entry['subject'][:24]}...: {field} is {cip113.get(field)}, the registry node "
            f"{policy['registry_utxo']} has {policy['cip113'][field]}")


def make_test_class(fixture, not_served=None, native_subjects=()):
    """
    The test class for one network. not_served maps a subject that has on-chain metadata but is not served to the
    reason; native_subjects are subjects of unregistered policies the API serves, so they must be NATIVE.
    """
    not_served = not_served or {}
    cases = _cases(fixture)
    expected_served = [(s, p) for s, p in cases if _has_metadata(s) and s["subject"] not in not_served]
    expected_404 = [(s, p) for s, p in cases if not _has_metadata(s) or s["subject"] in not_served]
    policies_by_id = {p["policy_id"]: p for p in fixture["policies"]}
    case_id = lambda case: case[0]["subject"][:16] + "-" + case[0]["subject"][56:][-16:]

    @allure.epic("V2 API")
    @allure.feature(f"CIP-113 programmable tokens ({fixture['network']})")
    @pytest.mark.v2
    @pytest.mark.cip113
    class TestV2Cip113:

        @allure.story("A programmable token with valid metadata is PROGRAMMABLE, with the registry node's extension")
        @pytest.mark.parametrize("case", expected_served, ids=case_id)
        def test_served_as_programmable(self, case):
            subject, policy = case
            resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{subject['subject']}")
            assert resp.status_code == 200, f"{subject['subject']}: expected 200, got {resp.status_code}"
            _assert_programmable(resp.json()["subject"], policy)

        @allure.story("A programmable token without valid metadata is not served")
        @pytest.mark.parametrize("case", expected_404, ids=case_id)
        def test_not_served_without_valid_metadata(self, case):
            subject, _ = case
            resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{subject['subject']}")
            reason = not_served.get(subject["subject"], "no CIP-26 or CIP-68 metadata on chain")
            assert resp.status_code == 404, f"{subject['subject']}: expected 404 ({reason}), got {resp.status_code}"

        @allure.story("The extension is also returned with show_cips_details")
        @pytest.mark.parametrize("case", expected_served, ids=case_id)
        def test_served_as_programmable_with_cips_details(self, case):
            subject, policy = case
            resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{subject['subject']}",
                                params={"show_cips_details": "true"})
            assert resp.status_code == 200
            entry = resp.json()["subject"]
            _assert_programmable(entry, policy)
            assert entry.get("standards"), "show_cips_details=true returned no standards"

        @allure.story("The batch query returns the same programmable tokens as the single queries")
        def test_batch_query(self):
            if not cases:
                pytest.skip("no programmable token is registered on this network")
            resp = requests.post(f"{API_BASE_URL}/api/v2/subjects/query",
                                 json={"subjects": [s["subject"] for s, _ in cases]})
            assert resp.status_code == 200
            returned = {entry["subject"]: entry for entry in resp.json()["subjects"]}
            assert set(returned) == {s["subject"] for s, _ in expected_served}
            for subject, entry in returned.items():
                _assert_programmable(entry, policies_by_id[subject[:56]])

        @allure.story("A token of a policy that is not registered is NATIVE, without extensions")
        @pytest.mark.parametrize("subject", list(native_subjects), ids=lambda s: s[:16])
        def test_unregistered_token_is_native(self, subject):
            assert subject[:56] not in policies_by_id
            resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{subject}")
            assert resp.status_code == 200
            entry = resp.json()["subject"]
            assert entry["type"] == "NATIVE"
            assert not entry.get("extensions")

    return TestV2Cip113
