"""
V2 API: CIP-113 programmable tokens on preprod. See shared/cip113_suite.py.

Most of the tokens registered on preprod are tests without metadata. NOT_SERVED lists those with on-chain metadata that
the registry rejects, with the reason.
"""

from shared.cip113_suite import load_fixture, make_test_class

NOT_SERVED = {
    # BRLT: the 333 datum has name, ticker and decimals but no description, which CIP-68 requires for label 333.
    # The same token under policy b75439f5... has a description and is served.
    "baddd482c787be3b2f11dd2cf916755bd7a30f0c41070011ec7190ee0014df1042524c54":
        "CIP-68 333 datum has no description",
}

TestV2Cip113 = make_test_class(load_fixture(__file__), not_served=NOT_SERVED)
