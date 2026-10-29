"""
V2 API: the validation contract of ADR-019 on preprod. See shared/validation_suite.py.

The subjects (fixtures/validation_sample.json, made by shared/generate_validation_sample.py) are CIP-26 entries and
CIP-68 tokens spread over the three kinds of result: no error, optional errors only, a mandatory error.
"""

import json
import os

from shared.validation_suite import make_test_class

with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures", "validation_sample.json")) as f:
    SAMPLE = [s["subject"] for s in json.load(f)["subjects"]]

TestV2Validation = make_test_class(SAMPLE)
