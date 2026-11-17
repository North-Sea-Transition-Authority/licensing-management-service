#!/bin/sh
# Publishes the database ERD, DDL SQL dump, and JSON schemas for the DWH to the public mirror repo.
# generate-dwh-docs.sh and the generateJsonSchema gradle task must be run first.

set -eu
cd "$(dirname "$0")/../.."

SCHEMA_DUMP_FILE=build/lms-schema.sql
OUTPUT_DIR=build/reports/schemaspy
JSON_SCHEMAS_DIR=build/json-schemas

# Push to public mirror as a Release
docker run --rm -v "$PWD:/w" -w /w \
  -e GH_TOKEN -e TARGET_REPO -e DRONE_BUILD_NUMBER -e DRONE_BRANCH \
  -e OUTPUT_DIR="$OUTPUT_DIR" -e JSON_SCHEMAS_DIR="$JSON_SCHEMAS_DIR" -e SCHEMA_DUMP_FILE="$SCHEMA_DUMP_FILE" \
  alpine:3.20 sh -euc '
    apk add -q --no-cache github-cli zip
    (cd "$OUTPUT_DIR" && zip -qr /tmp/lms-schema-browser.zip .)
    # Remove <a> tags from stand alone SVG
    sed -e "s#<a [^>]*>##g" -e "s#</a>##g" "$OUTPUT_DIR/diagrams/summary/relationships.real.large.svg" > /tmp/lms-erd.svg
    gh release create "lms-dwh-docs-$DRONE_BUILD_NUMBER" --repo "$TARGET_REPO" \
      --title "LMS schema information for $DRONE_BRANCH (build $DRONE_BUILD_NUMBER)" \
      --notes-file documentation/dwh/release-note.md \
      "/tmp/lms-erd.svg#Database ER diagram (svg)" \
      "/tmp/lms-schema-browser.zip#Interactive schema browser (zip)" \
      "$SCHEMA_DUMP_FILE#Full DDL schema dump (sql)" \
      "$JSON_SCHEMAS_DIR"/*.json
  '
