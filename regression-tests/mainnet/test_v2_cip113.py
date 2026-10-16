"""
V2 API: CIP-113 programmable tokens on mainnet. See shared/cip113_suite.py.

None of the tokens registered on mainnet has CIP-26 or CIP-68 metadata yet, so the API serves none of them; the suite
pins that, and it pins the extension as soon as one is given metadata and the fixture is regenerated.
"""

from shared.cip113_suite import load_fixture, make_test_class

from .conftest import _cip26_token_ids

TestV2Cip113 = make_test_class(load_fixture(__file__), native_subjects=_cip26_token_ids()[:5])
