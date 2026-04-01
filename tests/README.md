# End-to-End Tests

Python-based end-to-end regression tests for the Token Metadata Registry API.
Tests validate all V1 and V2 business endpoints against fixture data (JSON snapshots) captured from the PostgreSQL database.

The goal is to take up to 1000 CIP-26 tokens and 1000 CIP-68 tokens from the database, snapshot them as JSON fixtures, and then assert that the API returns correct data for every one of them. This provides a sanity baseline that can be run against any branch.

## Prerequisites

- Python 3.10+
- Running PostgreSQL instance with synced token metadata
- Running API instance (default `http://localhost:8080`)

## Setup

### 1. Create virtual environment and install dependencies

```bash
cd tests
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

The `requirements.txt` installs:
- `pytest` - test runner
- `allure-pytest` - rich web UI for viewing test results
- `requests` - HTTP client for API calls
- `psycopg2-binary` - PostgreSQL driver for fixture generation

### 2. Generate fixture snapshots from the database

The fixture generator connects to the database and exports up to 1000 CIP-26 tokens (from `metadata` + `logo` tables) and up to 1000 CIP-68 tokens (from `metadata_reference_nft` table) as JSON files.

```bash
# Make sure venv is activated
source venv/bin/activate

# Generate with default settings (connects to localhost:5432)
python end2end/mainnet/fixtures/generate_fixtures.py
```

This creates two files:
- `end2end/mainnet/fixtures/cip26_tokens.json` - up to 1000 CIP-26 token snapshots
- `end2end/mainnet/fixtures/cip68_tokens.json` - up to 1000 CIP-68 token snapshots

#### Customizing the database connection

All settings are configurable via environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `cf_token_metadata_registry` | Database name |
| `DB_USERNAME` | `cardano` | Database user |
| `DB_PASSWORD` | `metadata1337_` | Database password |
| `MAX_TOKENS` | `1000` | Max tokens to snapshot per CIP type |

Example with custom settings:

```bash
DB_HOST=mydb.example.com DB_PASSWORD=secret MAX_TOKENS=500 \
  python end2end/mainnet/fixtures/generate_fixtures.py
```

#### When to regenerate fixtures

- After the initial CIP-26 or CIP-68 sync completes (to capture all tokens)
- After switching to a different database or network
- When you want a fresh baseline for regression testing

## Running Tests

### Run all tests

```bash
cd end2end/mainnet
source ../../venv/bin/activate
python -m pytest -v
```

### Run with Allure report data

```bash
python -m pytest -v --alluredir=../../allure-results
```

### Filter by marker

```bash
python -m pytest -m v1           # V1 endpoints only
python -m pytest -m v2           # V2 endpoints only
python -m pytest -m cip26        # CIP-26 token tests
python -m pytest -m cip68        # CIP-68 token tests
python -m pytest -m batch        # Batch query tests
```

Markers can be combined:

```bash
python -m pytest -m "v2 and cip26"       # V2 CIP-26 tests only
python -m pytest -m "v1 and batch"       # V1 batch tests only
```

### Targeting a different API instance

```bash
API_BASE_URL=http://myhost:8080 python -m pytest -v
```

Default is `http://localhost:8080`.

## Viewing Results with Allure (Web UI)

Allure provides an interactive web dashboard with charts, test history, categorization, and detailed failure reports.

### Install Allure CLI

```bash
# Option A: via npm (easiest)
npm install -g allure-commandline

# Option B: manual download (requires Java)
sudo apt install default-jre
wget https://github.com/allure-framework/allure2/releases/download/2.30.0/allure-2.30.0.tgz
tar -xzf allure-2.30.0.tgz
export PATH=$PWD/allure-2.30.0/bin:$PATH
```

### View the report

```bash
# From the tests/ directory
allure serve allure-results
```

This opens a browser with the interactive report. You can see pass/fail counts, drill into individual test cases, and view failure details.

## Full Workflow Example

Complete example from scratch:

```bash
# 1. Navigate to tests directory
cd tests

# 2. Set up Python virtual environment
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 3. Generate fixtures (ensure DB is running and synced)
python end2end/mainnet/fixtures/generate_fixtures.py

# 4. Run tests (ensure API is running)
cd end2end/mainnet
python -m pytest -v --alluredir=../../allure-results

# 5. View results in browser
cd ../..
allure serve allure-results
```

### Regenerating after CIP-68 sync completes

Once the blockchain sync finishes and CIP-68 tokens appear in the `metadata_reference_nft` table:

```bash
cd tests
source venv/bin/activate

# Re-snapshot (captures up to 1000 CIP-68 tokens)
python end2end/mainnet/fixtures/generate_fixtures.py

# Re-run tests (CIP-68 tests will now run instead of being skipped)
cd end2end/mainnet
python -m pytest -v --alluredir=../../allure-results
```

## Test Coverage

| Endpoint | Method | Description |
|---|---|---|
| `/metadata/{subject}` | GET | All CIP-26 tokens parametrized, field-by-field assertion against DB |
| `/metadata/{subject}/properties/{property}` | GET | Sample of 50 tokens, each property type tested individually |
| `/metadata/query` | POST | Batch queries, property filters, mixed known/unknown subjects, edge cases |
| `/api/v2/subjects/{subject}` | GET | All CIP-26 + CIP-68 tokens, `query_priority`, `show_cips_details`, property filter |
| `/api/v2/subjects/query` | POST | Batch with priority combos, CIP details toggle, mixed CIP types, validity filtering |

## Project Structure

```
tests/
├── requirements.txt                          # Python dependencies
├── README.md                                 # This file
└── end2end/
    └── mainnet/
        ├── conftest.py                       # Shared pytest fixtures and helpers
        ├── pytest.ini                        # Test markers configuration
        ├── fixtures/
        │   ├── generate_fixtures.py          # DB -> JSON snapshot script
        │   ├── cip26_tokens.json             # CIP-26 token snapshots (generated)
        │   └── cip68_tokens.json             # CIP-68 token snapshots (generated)
        ├── test_v1_get_subject.py            # V1 GET /metadata/{subject}
        ├── test_v1_get_property.py           # V1 GET /metadata/{subject}/properties/{property}
        ├── test_v1_batch_query.py            # V1 POST /metadata/query
        ├── test_v2_get_subject.py            # V2 GET /api/v2/subjects/{subject}
        └── test_v2_batch_query.py            # V2 POST /api/v2/subjects/query
```
