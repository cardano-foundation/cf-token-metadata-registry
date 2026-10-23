"""
Generate <network>/fixtures/validation_sample.json: subjects for shared/validation_suite.py.

The contract suite needs only subjects the API serves, with every kind of result among them, not expected values. This
picks CIP-26 entries and CIP-68 tokens from a database synced with ADR-019 (validation errors stored), spread over the
three kinds of result: no error, optional errors only, a mandatory error. A CIP-68 token is kept under the first of its
333/444 subjects the API serves, and rich fungible tokens (a datum with an image) are added under their 444 subject.

Usage:
    DB_HOST=... DB_PORT=... DB_PASSWORD=... API_BASE_URL=... \\
        uv run python shared/generate_validation_sample.py preprod [--per-kind 8] [--cip26 25]
"""

import argparse
import json
import os
from datetime import datetime, timezone

import psycopg2
import psycopg2.extras
import requests

API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
REFERENCE_PREFIX = "000643b0"
FT_PREFIX = "0014df10"
RFT_PREFIX = "001bc280"

KIND_SQL = """
    case when validation_errors is null then 'unchecked'
         when jsonb_array_length(validation_errors) = 0 then 'clean'
         when validation_errors @> '[{"field_category": "MANDATORY"}]' then 'mandatory'
         else 'optional' end
"""


def connect():
    return psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        dbname=os.getenv("DB_NAME", "cf_token_metadata_registry"),
        user=os.getenv("DB_USERNAME", "cardano"),
        password=os.getenv("DB_PASSWORD", ""),
    )


def spread(rows, n):
    step = max(1, len(rows) // n)
    return rows[::step][:n]


def served_kind(subject):
    """The kind of result the API serves the subject with, or None when it is not served. A CIP-68 subject gets only the
    errors of its own label, so its kind can differ from that of its row."""
    resp = requests.get(f"{API_BASE_URL}/api/v2/subjects/{subject}", timeout=30)
    if resp.status_code != 200:
        return None
    errors = resp.json()["subject"].get("validation_errors")
    if errors is None:
        return "unchecked"
    if not errors:
        return "clean"
    return "mandatory" if any(e["field_category"] == "MANDATORY" for e in errors) else "optional"


def cip26_sample(cur, n):
    cur.execute(f"select subject, {KIND_SQL} as kind from metadata order by subject")
    rows = cur.fetchall()
    # entries with errors first (a third of the sample each, when there are that many), the rest without
    picked = spread([r for r in rows if r["kind"] == "optional"], n // 3) + spread([r for r in rows if r["kind"] == "mandatory"], n // 3)
    picked += spread([r for r in rows if r["kind"] == "clean"], n - len(picked))
    kinds = [(r["subject"], served_kind(r["subject"])) for r in picked]
    return [{"subject": subject, "standard": "CIP-26", "kind": kind} for subject, kind in kinds if kind]


def cip68_sample(cur, per_kind):
    cur.execute(f"""
        select policy_id, asset_name, image, validation_errors, {KIND_SQL} as kind
        from (select distinct on (policy_id, asset_name) * from metadata_reference_nft
              order by policy_id, asset_name, slot desc) latest
        where asset_name like '{REFERENCE_PREFIX}%'
        order by policy_id, asset_name""")
    rows = cur.fetchall()
    picked = []
    for kind in ("clean", "optional", "mandatory"):
        candidates = spread([r for r in rows if r["kind"] == kind], per_kind * 3)
        found = []
        for row in candidates:
            base = row["asset_name"][len(REFERENCE_PREFIX):]
            labels = {e.get("sub_standard") for e in row["validation_errors"] or []}
            prefixes = (RFT_PREFIX, FT_PREFIX) if "444" in labels else (FT_PREFIX, RFT_PREFIX)
            for prefix in prefixes:
                subject = row["policy_id"] + prefix + base
                if served_kind(subject) == kind:
                    found.append({"subject": subject, "standard": "CIP-68", "kind": kind})
                    break
            if len(found) == per_kind:
                break
        picked += found
    return picked


def rft_sample(cur, per_kind):
    """Rich fungible tokens (label 444), which the 333-first search above rarely finds: a datum with an image, served
    under its 444 subject."""
    cur.execute(f"""
        select policy_id, asset_name, {KIND_SQL} as kind
        from (select distinct on (policy_id, asset_name) * from metadata_reference_nft
              order by policy_id, asset_name, slot desc) latest
        where asset_name like '{REFERENCE_PREFIX}%'
          and (image is not null or validation_errors @> '[{{"sub_standard": "444"}}]')
        order by policy_id, asset_name""")
    rows = cur.fetchall()
    # rows whose errors were found for label 444 first: they are the ones a 444 subject can be served with errors
    rows = [r for r in rows if r["kind"] != "clean"] + [r for r in rows if r["kind"] == "clean"]
    found = {"clean": [], "optional": [], "mandatory": []}
    for row in rows:
        subject = row["policy_id"] + RFT_PREFIX + row["asset_name"][len(REFERENCE_PREFIX):]
        kind = served_kind(subject)
        if kind in found and len(found[kind]) < per_kind:
            found[kind].append({"subject": subject, "standard": "CIP-68 (444)", "kind": kind})
        if all(len(v) == per_kind for v in found.values()):
            break
    return [s for kind in found.values() for s in kind]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("network")
    parser.add_argument("--per-kind", type=int, default=8, help="CIP-68 tokens per kind of result")
    parser.add_argument("--rft", type=int, default=3, help="CIP-68 rich fungible tokens (444) per kind of result")
    parser.add_argument("--cip26", type=int, default=25, help="CIP-26 entries")
    args = parser.parse_args()

    with connect() as conn, conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        subjects = cip26_sample(cur, args.cip26) + cip68_sample(cur, args.per_kind)
        subjects += [s for s in rft_sample(cur, args.rft) if s["subject"] not in {x["subject"] for x in subjects}]

    path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), args.network, "fixtures",
                        "validation_sample.json")
    with open(path, "w") as f:
        json.dump({
            "network": args.network,
            "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "source": "metadata and metadata_reference_nft of a registry synced with ADR-019, checked against " + API_BASE_URL,
            "subjects": subjects,
        }, f, indent=2)
        f.write("\n")
    kinds = {}
    for s in subjects:
        kinds[(s["standard"], s["kind"])] = kinds.get((s["standard"], s["kind"]), 0) + 1
    print(f"wrote {len(subjects)} subjects to {path}: {kinds}")


if __name__ == "__main__":
    main()
