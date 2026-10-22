"""
V2 API: a CIP-68 333 subject whose user token was never minted is not served (ADR-022).

The reference NFT of the same name holds metadata, but it does not say that the 333 token exists: these subjects belong to
NFTs (a 222 token), to rich fungible tokens (a 444 token) or to nothing. They were part of the CIP-68 baseline until the
registry learned which user tokens exist (mainnet/fixtures/cip68_never_minted_subjects.json, checked on Koios).
"""

import json
import os

import allure
import pytest
import requests

from .conftest import API_BASE_URL

with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures", "cip68_never_minted_subjects.json")) as f:
    NEVER_MINTED = json.load(f)["subjects"]


@allure.epic("V2 API")
@allure.feature("CIP-68 user tokens (ADR-022)")
@pytest.mark.v2
@pytest.mark.cip68
@pytest.mark.validation
class TestV2Cip68NeverMinted:

    @allure.story("A 333 subject whose token was never minted is a 404")
    @pytest.mark.parametrize("token", NEVER_MINTED, ids=lambda t: f"{t['minted_as'].split()[0]}-{t['subject'][:16]}")
    def test_never_minted_subject_is_not_served(self, token):
        resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{token['subject']}")
        assert resp.status_code == 404, f"{token['subject']} ({token['minted_as']}): {resp.status_code}"

    @allure.story("A batch leaves out the subjects whose token was never minted")
    def test_batch_leaves_them_out(self):
        subjects = [t["subject"] for t in NEVER_MINTED[:50]]
        resp = requests.post(f"{API_BASE_URL}/api/v2/subjects/query", json={"subjects": subjects})
        assert resp.status_code == 200
        assert resp.json()["subjects"] == []
