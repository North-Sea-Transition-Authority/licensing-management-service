#!/bin/sh
# Generates a database ERD and diffable DDL SQL dump for the Data Warehouse
 
set -eu
cd "$(dirname "$0")/../.."

SCHEMA_DUMP_FILE=build/lms-schema.sql
OUTPUT_DIR=build/reports/schemaspy
COMPOSE="docker compose -p lms-schema-diagram -f documentation/dwh/schema-diagram.compose.yml"

trap '$COMPOSE down -v' EXIT

mkdir -p "$OUTPUT_DIR" && chmod 777 "$OUTPUT_DIR"

# Generate ERDs
$COMPOSE run --rm -T schemaspy

# Generate sql dump so DWH can diff between releases and see what's changed
$COMPOSE exec -T db pg_dump -U schemaspy -d lms --schema=lms --schema-only --no-owner --no-privileges \
  --restrict-key=lmsschema > "$SCHEMA_DUMP_FILE"
