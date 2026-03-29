#!/usr/bin/env python3
"""
Generate test fixture JSON files from the mainnet database.

Queries the PostgreSQL database for CIP-26 and CIP-68 token metadata
and writes them as JSON fixture files for end-to-end tests.

Usage:
    python generate_fixtures.py

Environment variables:
    DB_HOST     (default: localhost)
    DB_PORT     (default: 5432)
    DB_NAME     (default: cf_token_metadata_registry)
    DB_USERNAME (default: cardano)
    DB_PASSWORD (default: metadata1337_)
    MAX_TOKENS  (default: 1000)
"""

import json
import os
import sys
from datetime import datetime

import psycopg2
import psycopg2.extras


def get_db_connection():
    return psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        dbname=os.getenv("DB_NAME", "cf_token_metadata_registry"),
        user=os.getenv("DB_USERNAME", "cardano"),
        password=os.getenv("DB_PASSWORD", "metadata1337_"),
    )


def fetch_cip26_tokens(conn, max_tokens=1000):
    """Fetch CIP-26 tokens from metadata + logo tables."""
    query = """
        SELECT
            m.subject,
            m.policy,
            m.name,
            m.ticker,
            m.url,
            m.description,
            m.decimals,
            m.updated,
            m.updated_by,
            l.logo
        FROM metadata m
        LEFT JOIN logo l ON m.subject = l.subject
        ORDER BY m.subject
        LIMIT %s
    """
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(query, (max_tokens,))
        rows = cur.fetchall()

    tokens = []
    for row in rows:
        token = {
            "subject": row["subject"],
            "policy": row["policy"],
            "name": row["name"],
            "ticker": row["ticker"],
            "url": row["url"],
            "description": row["description"],
            "decimals": row["decimals"],
            "updated": row["updated"].isoformat() if row["updated"] else None,
            "updated_by": row["updated_by"],
            "has_logo": row["logo"] is not None and len(row["logo"]) > 0,
        }
        # properties JSONB is excluded from fixtures to keep file size small
        # (it contains logo blobs with signatures that can be 80KB+ per token)
        tokens.append(token)

    return tokens


def fetch_cip68_tokens(conn, max_tokens=1000):
    """Fetch CIP-68 tokens from metadata_reference_nft table (latest slot per asset)."""
    query = """
        SELECT DISTINCT ON (policy_id, asset_name)
            policy_id,
            asset_name,
            slot,
            name,
            description,
            ticker,
            url,
            decimals,
            logo,
            version
        FROM metadata_reference_nft
        ORDER BY policy_id, asset_name, slot DESC
        LIMIT %s
    """
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(query, (max_tokens,))
        rows = cur.fetchall()

    tokens = []
    for row in rows:
        # Build the fungible token subject: policyId + "0014df10" + suffix
        # The DB stores reference NFT asset names with "000643b0" prefix
        asset_name = row["asset_name"]
        ref_prefix = "000643b0"
        ft_prefix = "0014df10"
        if asset_name.startswith(ref_prefix):
            ft_asset_name = ft_prefix + asset_name[len(ref_prefix):]
        else:
            ft_asset_name = asset_name

        token = {
            "subject": row["policy_id"] + ft_asset_name,
            "policy_id": row["policy_id"],
            "asset_name": row["asset_name"],
            "ft_asset_name": ft_asset_name,
            "slot": row["slot"],
            "name": row["name"],
            "description": row["description"],
            "ticker": row["ticker"],
            "url": row["url"],
            "decimals": row["decimals"],
            "has_logo": row["logo"] is not None and len(row["logo"]) > 0,
            "version": row["version"],
        }
        tokens.append(token)

    return tokens


def main():
    max_tokens = int(os.getenv("MAX_TOKENS", "1000"))
    script_dir = os.path.dirname(os.path.abspath(__file__))

    conn = get_db_connection()
    try:
        print(f"Fetching up to {max_tokens} CIP-26 tokens...")
        cip26_tokens = fetch_cip26_tokens(conn, max_tokens)
        print(f"  Found {len(cip26_tokens)} CIP-26 tokens")

        cip26_path = os.path.join(script_dir, "cip26_tokens.json")
        with open(cip26_path, "w") as f:
            json.dump(cip26_tokens, f, indent=2)
        print(f"  Written to {cip26_path}")

        print(f"Fetching up to {max_tokens} CIP-68 tokens...")
        cip68_tokens = fetch_cip68_tokens(conn, max_tokens)
        print(f"  Found {len(cip68_tokens)} CIP-68 tokens")

        cip68_path = os.path.join(script_dir, "cip68_tokens.json")
        with open(cip68_path, "w") as f:
            json.dump(cip68_tokens, f, indent=2)
        print(f"  Written to {cip68_path}")

    finally:
        conn.close()

    print("Done!")


if __name__ == "__main__":
    main()
