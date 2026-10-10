"""
V1 API: deleted registry entries must not be served (issue #97).

Guards the invariant that a CIP-26 token whose mapping file was removed from the
upstream cardano-token-registry is no longer served by the API.

The fixture `cip26_removed_tokens.json` was mined from the upstream repository's
git history: every entry was once a canonical mapping (filename == inner subject,
valid name + description, so the sync WOULD have indexed it) whose file was later
deleted and is still absent at HEAD. It includes the exact token from issue #97
(subject 2d8cee2c...6672414441, "ADAFR Pool Reward ADA", removed upstream in
cardano-token-registry PR #8014).

This fixture is a frozen baseline like the others — do not regenerate during a
test run. If one of these tokens is ever legitimately re-added upstream, remove
that single entry deliberately.

Only V1 endpoints are asserted: V1 is the pure CIP-26 view. V2 may legitimately
serve some of these subjects from on-chain CIP-68 data (e.g. subjects carrying
the 0014df10 fungible-token prefix), so a V2 404 assertion would be flaky by design.
"""

import allure
import pytest
import requests

from .conftest import _load_json, API_BASE_URL

REMOVED_TOKENS = _load_json("cip26_removed_tokens.json")
REMOVED_SUBJECTS = [t["subject"] for t in REMOVED_TOKENS]


@allure.epic("V1 API")
@allure.feature("Removed registry entries")
@pytest.mark.v1
@pytest.mark.cip26
@pytest.mark.deletion
class TestV1RemovedSubjects:

    @allure.story("Subject deleted from the upstream registry returns 204")
    @pytest.mark.parametrize("subject", REMOVED_SUBJECTS, ids=lambda s: s[:16])
    def test_removed_subject_returns_204(self, subject):
        resp = requests.get(f"{API_BASE_URL}/metadata/{subject}")
        assert resp.status_code == 204, (
            f"Subject {subject[:24]}... was deleted from the upstream registry "
            f"but the API still serves it (HTTP {resp.status_code}). "
            f"See issue #97 — deletions must be propagated to the local DB."
        )

    @allure.story("Batch query does not return subjects deleted upstream")
    def test_batch_query_excludes_removed_subjects(self):
        resp = requests.post(
            f"{API_BASE_URL}/metadata/query",
            json={"subjects": REMOVED_SUBJECTS},
        )
        assert resp.status_code == 200
        data = resp.json()
        returned = {s["subject"] for s in data.get("subjects", [])}
        leaked = returned & set(REMOVED_SUBJECTS)
        assert not leaked, (
            f"{len(leaked)} deleted subject(s) still returned by batch query, "
            f"e.g. {sorted(leaked)[0][:24]}..."
        )
