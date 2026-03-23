#!/bin/bash
set -euo pipefail

PORT="${1:-8080}"
BASE_URL="http://localhost:${PORT}/__admin/message-mappings"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MESSAGES_DIR="${SCRIPT_DIR}/messages"

loaded=0
failed=0

for stub_file in "${MESSAGES_DIR}"/*.json; do
  filename=$(basename "$stub_file")

  if python3 -c "import json,sys; d=json.load(open(sys.argv[1])); sys.exit(0 if 'messageMappings' in d else 1)" "$stub_file" 2>/dev/null; then
    count=$(python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(len(d['messageMappings']))" "$stub_file")
    for i in $(seq 0 $((count - 1))); do
      single_stub=$(python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(json.dumps(d['messageMappings'][$i]))" "$stub_file" "$i")
      stub_name=$(echo "$single_stub" | python3 -c "import json,sys; print(json.loads(sys.stdin.read()).get('name','unnamed'))")
      response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL" \
        -H "Content-Type: application/json" \
        -d "$single_stub")
      if [ "$response" = "201" ]; then
        echo "  Loaded: ${stub_name} (from ${filename})"
        loaded=$((loaded + 1))
      else
        echo "  FAILED: ${stub_name} (from ${filename}) — HTTP ${response}"
        failed=$((failed + 1))
      fi
    done
  else
    stub_name=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1])).get('name','unnamed'))" "$stub_file")
    response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL" \
      -H "Content-Type: application/json" \
      -d @"$stub_file")
    if [ "$response" = "201" ]; then
      echo "  Loaded: ${stub_name} (from ${filename})"
      loaded=$((loaded + 1))
    else
      echo "  FAILED: ${stub_name} (from ${filename}) — HTTP ${response}"
      failed=$((failed + 1))
    fi
  fi
done

echo ""
echo "Stub loading complete: ${loaded} loaded, ${failed} failed."

if [ "$failed" -gt 0 ]; then
  exit 1
fi
