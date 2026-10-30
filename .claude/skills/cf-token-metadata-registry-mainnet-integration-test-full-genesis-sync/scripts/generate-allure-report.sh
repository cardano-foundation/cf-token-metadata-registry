#!/usr/bin/env bash
# Build the Allure report of the last run from regression-tests/allure-results
# into regression-tests/allure-report, and print its totals. Run it after
# run-cip26-tests.sh and run-cip68-tests.sh; open the report with the command
# it prints (the report must be served, not opened as a file).
#
# Uses the `allure` CLI when it is installed, otherwise allure-commandline from
# npm through npx. Both need Java (JAVA_HOME or java on the PATH).

set -euo pipefail
# shellcheck disable=SC1091
source "$(dirname "${BASH_SOURCE[0]}")/load-config.sh"

PROJECT_ROOT="$(cd "$SKILL_DIR/../../.." && pwd)"
REGRESSION_DIR="$PROJECT_ROOT/regression-tests"
RESULTS_DIR="$REGRESSION_DIR/allure-results"
REPORT_DIR="$REGRESSION_DIR/allure-report"

if ! compgen -G "$RESULTS_DIR/*-result.json" > /dev/null; then
  echo "ERROR: no Allure results in $RESULTS_DIR — run the regression tests first." >&2
  exit 1
fi

if command -v allure > /dev/null; then
  ALLURE=(allure)
else
  ALLURE=(npx -y allure-commandline@2.30.0)
fi

echo "[allure] generating $REPORT_DIR from $RESULTS_DIR..."
"${ALLURE[@]}" generate --clean "$RESULTS_DIR" -o "$REPORT_DIR"

python3 - "$REPORT_DIR/widgets/summary.json" <<'EOF'
import json, sys
summary = json.load(open(sys.argv[1]))
stats = summary["statistic"]
print("[allure] total {total}: passed {passed}, failed {failed}, broken {broken}, skipped {skipped}".format(**stats))
EOF

echo "[allure] open with: ${ALLURE[*]} open $REPORT_DIR"
