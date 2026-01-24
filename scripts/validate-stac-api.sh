#!/bin/bash
#
# STAC API Validator Script
#
# This script validates the spring-stac server against the official STAC API specification.
# The server must be running before executing this script.
#
# Usage:
#   ./scripts/validate-stac-api.sh [ROOT_URL]
#
# Arguments:
#   ROOT_URL - Base URL of the STAC API server (default: http://localhost:8080)
#
# Prerequisites:
#   - stac-api-validator installed: pip install stac-api-validator
#   - Spring STAC server running: ./gradlew bootRun
#
# Environment Variables:
#   JAVA_HOME - Path to JDK 21 (e.g., ~/.local/jdk)
#

set -e

# Set JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ] && [ -d "$HOME/.local/jdk" ]; then
    export JAVA_HOME="$HOME/.local/jdk"
fi

ROOT_URL="${1:-http://localhost:8080}"

echo "=================================================="
echo "STAC API Validator"
echo "=================================================="
echo "Target URL: ${ROOT_URL}"
echo ""

# Check if validator is installed
if ! command -v stac-api-validator &> /dev/null; then
    echo "ERROR: stac-api-validator not found"
    echo "Install with: pip install stac-api-validator"
    exit 1
fi

# Check if server is running
echo "Checking server availability..."
if ! curl -s -o /dev/null -w "%{http_code}" "${ROOT_URL}" | grep -q "200"; then
    echo "ERROR: Server not responding at ${ROOT_URL}"
    echo "Start the server with: ./gradlew bootRun"
    exit 1
fi
echo "Server is responding."
echo ""

# Run validation with all supported conformance classes
echo "Running STAC API validation..."
echo ""

stac-api-validator \
    --root-url "${ROOT_URL}" \
    --conformance core \
    --conformance collections \
    --conformance item-search \
    --conformance item-search#filter \
    --conformance item-search#sort \
    --conformance item-search#fields \
    --conformance item-search#context \
    --conformance features \
    --conformance features#transaction \
    "$@"

echo ""
echo "=================================================="
echo "Validation complete!"
echo "=================================================="
