#!/usr/bin/env python3
"""
Generate the CIP-68 rich fungible token (RFT, label 444) fixture of a network from the chain, through Koios.

The expected values come from the chain, not from the registry's database, so the suite checks the indexer against an
independent source. For every label-444 asset (user token prefix 001bc280), the generator reads its current 444 datum
and keeps the RFTs the registry serves with the same values on any build that serves RFTs (ADR-018):

- the datum has a name, a description and an image that is a URI with a scheme CIP-68 allows (https, ipfs, ar, data);
- name, description and ticker are read as the registry reads them: only a plain byte string counts (a Plutus list
  of byte chunks is treated as absent); the image may also be a list of chunks, joined (a CIP-68 uri), up to 64 KiB;
- the version is one CIP-68 defines (1 to 4);
- the reference NFT (000643b0) exists and sits at an output with an inline datum. CIP-68 metadata is an inline datum
  (CIP-32), and the registry reads nothing else; Koios also resolves a datum hash, and still shows the last datum of a
  burned reference NFT, so both are checked here.

RFTs without a description are left out: builds before RFT support skipped their datums, so a database that has not
been re-indexed since does not have them. The committed fixture also holds 13 RFTs without a description added by hand
(see test_v2_cip68_rft.py): regenerating it drops them, so add them back. The image is the expected `logo`: an RFT has no logo in CIP-68, and the
registry serves its image as one.

Usage:
    uv run python shared/generate_cip68_rft_fixtures.py mainnet

Writes <network>/fixtures/cip68_rft_tokens.json. Regenerate it on purpose, never as part of a test run: the committed
file is the baseline. Before committing a regenerated file, run the suite against an instance at tip; a token whose
expected values differ there is either removed with a reason or is a bug.
"""

import json
import os
import re
import sys
import time
import urllib.request
from datetime import datetime, timezone

NETWORKS = {
    "mainnet": "https://api.koios.rest/api/v1",
    "preprod": "https://preprod.koios.rest/api/v1",
}
RFT_PREFIX = "001bc280"
REFERENCE_NFT_PREFIX = "000643b0"
ALLOWED_URI = re.compile(r"^(https|ipfs|ar|data):.+", re.IGNORECASE | re.DOTALL)
TEXT_FIELDS = ("name", "description", "ticker")
URI_MAX_BYTES = 64 * 1024  # Cip68FTDatumParser.URI_MAX_BYTES
BATCH = 25


def koios(base, path, body=None, headers=None):
    for attempt in range(8):
        try:
            data = json.dumps(body).encode() if body is not None else None
            request = urllib.request.Request(f"{base}/{path}", data=data, headers={
                "content-type": "application/json", "accept": "application/json", **(headers or {})})
            with urllib.request.urlopen(request, timeout=240) as response:
                return json.load(response)
        except Exception as e:  # Koios is a public service: retry on a transient error or a rate limit
            if attempt == 7:
                raise
            print(f"  retrying {path} after {e}", file=sys.stderr)
            time.sleep(10 * (attempt + 1))


class Unrepresentable(Exception):
    pass


def decode(raw):
    """Bytes as the registry decodes them: UTF-8 without NUL characters; anything else is left out of the fixture."""
    try:
        return raw.decode("utf-8").replace("\0", "")
    except UnicodeDecodeError as e:
        raise Unrepresentable(f"not UTF-8: {e}")


def text(value):
    """A name, description or ticker: only a plain byte string, as Cip68FTDatumParser.getStringProperty reads it."""
    return decode(bytes.fromhex(value["bytes"])) if "bytes" in value else None


def uri(value):
    """An image: a byte string or a list of byte-string chunks, as Cip68FTDatumParser.getStringOrChunkedProperty."""
    if "bytes" in value:
        raw = bytes.fromhex(value["bytes"])
    elif "list" in value:
        raw = b"".join(bytes.fromhex(chunk["bytes"]) for chunk in value["list"] if "bytes" in chunk)
        if not raw:
            return None
    else:
        return None
    return decode(raw) if len(raw) <= URI_MAX_BYTES else None


def parse_rft_datum(datum):
    """The served fields of a 444 datum shown by Koios, or raises Unrepresentable/ValueError with the reason."""
    fields = datum.get("fields") or []
    if len(fields) < 2 or "map" not in fields[0] or "int" not in fields[1]:
        raise Unrepresentable("not a CIP-68 Constr 0 [metadata, version, ...]")
    version = fields[1]["int"]
    if not 1 <= version <= 4:
        raise ValueError(f"version {version} is not one CIP-68 defines")
    properties = {}
    for entry in fields[0]["map"]:
        key = entry["k"].get("bytes")
        if key is None:
            continue
        try:
            key = bytes.fromhex(key).decode("utf-8")
        except UnicodeDecodeError:
            continue
        if key == "721":
            raise Unrepresentable("nested 721 metadata")
        if key in TEXT_FIELDS:
            properties[key] = text(entry["v"])
        elif key == "image":
            properties[key] = uri(entry["v"])
        elif key == "decimals":
            if "int" not in entry["v"]:
                raise Unrepresentable("decimals is not an integer")
            properties[key] = entry["v"]["int"]
    if not properties.get("name"):
        raise ValueError("no name")
    if properties.get("description") is None:
        raise ValueError("no description")
    if not properties.get("image"):
        raise ValueError("no image")
    if not ALLOWED_URI.match(properties["image"]):
        raise ValueError("image is not a URI with a scheme CIP-68 allows")
    return {
        "name": properties["name"],
        "description": properties["description"],
        "ticker": properties.get("ticker"),
        "decimals": properties.get("decimals"),
        "image": properties["image"],
        "version": version,
    }


def main():
    network = sys.argv[1] if len(sys.argv) > 1 else "mainnet"
    base = NETWORKS[network]

    assets = []
    for offset in range(0, 1_000_000, 1000):
        page = koios(base, f"asset_list?select=policy_id,asset_name&asset_name=like.{RFT_PREFIX}*",
                     headers={"Range": f"{offset}-{offset + 999}"})
        assets += page
        if len(page) < 1000:
            break
    print(f"{network}: {len(assets)} label-444 assets")

    infos = []
    for i in range(0, len(assets), BATCH):
        infos += koios(base, "asset_info", {"_asset_list": [[a["policy_id"], a["asset_name"]] for a in assets[i:i + BATCH]]})
        time.sleep(1.2)

    tokens, skipped = [], {}
    for info in infos:
        datum = (info.get("cip68_metadata") or {}).get("444")
        if not datum:
            skipped["no 444 datum"] = skipped.get("no 444 datum", 0) + 1
            continue
        try:
            fields = parse_rft_datum(datum)
        except (Unrepresentable, ValueError) as e:
            reason = re.sub(r":.*", "", str(e))
            skipped[reason] = skipped.get(reason, 0) + 1
            continue
        tokens.append({"subject": info["policy_id"] + info["asset_name"], "policy_id": info["policy_id"],
                       "asset_name": info["asset_name"], **fields})

    # The reference NFT must be live and hold an inline datum
    references = {}
    for i in range(0, len(tokens), BATCH):
        batch = [[t["policy_id"], REFERENCE_NFT_PREFIX + t["asset_name"][len(RFT_PREFIX):]] for t in tokens[i:i + BATCH]]
        for utxo in koios(base, "asset_utxos", {"_asset_list": batch, "_extended": True}):
            for asset in utxo.get("asset_list") or []:
                if asset["asset_name"].startswith(REFERENCE_NFT_PREFIX):
                    references[(asset["policy_id"], asset["asset_name"])] = utxo.get("inline_datum") is not None
        time.sleep(1.2)
    kept = []
    for t in tokens:
        inline = references.get((t["policy_id"], REFERENCE_NFT_PREFIX + t["asset_name"][len(RFT_PREFIX):]))
        if inline is None:
            skipped["reference NFT burned"] = skipped.get("reference NFT burned", 0) + 1
        elif not inline:
            skipped["reference NFT datum is a datum hash, not inline"] = skipped.get("reference NFT datum is a datum hash, not inline", 0) + 1
        else:
            kept.append(t)
    tokens = sorted(kept, key=lambda t: t["subject"])

    fixture = {
        "network": network,
        "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source": f"{base} asset_list (asset_name like {RFT_PREFIX}*) + asset_info cip68_metadata.444",
        "skipped": dict(sorted(skipped.items())),
        "tokens": tokens,
    }
    path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), network, "fixtures",
                        "cip68_rft_tokens.json")
    with open(path, "w") as f:
        json.dump(fixture, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"{network}: {len(tokens)} RFTs with a description -> {path}")
    for reason, count in fixture["skipped"].items():
        print(f"  skipped {count}: {reason}")


if __name__ == "__main__":
    main()
