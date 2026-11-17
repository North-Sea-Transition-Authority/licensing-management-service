# LMS schema documentation

Auto-generated documentation of the `lms` database schema used by the Licensing Management Service, for this release.

## Database ER diagram (`lms-erd.svg`)

An entity relationship diagram of the `lms` schema, showing all columns and the foreign key relationships between tables.
For more detailed information such as column types, use the interactive schema browser below.

## Interactive schema browser (`lms-schema-browser.zip`)

A interactive web UI describing the schema in detail. Unzip it and open `index.html` in your browser.

It includes a page for every table listing its columns, types, nullability, defaults, indexes and constraints,
along with diagrams of each table and the tables it is related to.

## Full DDL schema dump (`lms-schema.sql`)

The SQL that creates the schema: tables, columns, constraints, indexes and sequences.
Objects are listed in a consistent order, so the file can be diffed against the one from a previous release to see what has changed.

## JSON schemas (`*.json`)

[JSON Schema](https://json-schema.org/overview/what-is-jsonschema) definitions for the structured JSON stored in `jsonb` columns.

Each schema describes the whole table row as the application models it, with property names in camelCase (e.g. `changeOrder` for `change_order`).
Only the property matching the `jsonb` column is relevant, the other columns are best taken from the schema browser.
