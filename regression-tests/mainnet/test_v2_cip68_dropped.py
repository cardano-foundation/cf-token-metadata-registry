"""
V2 API: CIP-68 tokens whose on-chain metadata the registry no longer indexes must not be served from CIP-68.

The fixture `cip68_dropped_tokens.json` holds tokens taken out of `cip68_tokens.json` when the CIP-68 rules became
stricter (#107), each with the reason. Their datum breaks what CIP-68 requires, so with the CIP_68 priority alone they
are not found. Under the default priority they may still be served from CIP-26, which is why only the CIP_68-only
query is asserted.

This fixture is a frozen baseline like the others: do not regenerate it during a test run. Add a token deliberately,
with the reason, when a rule change drops it.
"""

import allure
import pytest
import requests

from .conftest import _load_json, API_BASE_URL

DROPPED_TOKENS = _load_json("cip68_dropped_tokens.json")


@allure.epic("V2 API")
@allure.feature("CIP-68 tokens dropped by the CIP-68 rules")
@pytest.mark.v2
@pytest.mark.cip68
class TestV2Cip68Dropped:

    @allure.story("A token whose datum breaks CIP-68 is not served from CIP-68")
    @pytest.mark.parametrize("token", DROPPED_TOKENS, ids=lambda t: t["subject"][:16])
    def test_not_served_from_cip68(self, token):
        resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{token['subject']}",
                            params={"query_priority": "CIP_68"})
        assert resp.status_code == 404, (
            f"{token['name']} ({token['subject'][:16]}...): expected 404, because {token['dropped_because']}; "
            f"got {resp.status_code}")
