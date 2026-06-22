CREATE TABLE IF NOT EXISTS lms.cs_licence_migration_extract (
    licence_ref TEXT,
    licence_number TEXT,
    responsible_orgs TEXT,
    status TEXT,
    status_date TEXT
);

CREATE TABLE IF NOT EXISTS lms.cs_licence_org_mapping (
    cs_extract_responsible_organisation TEXT,
    organisation_unit_id INTEGER,
    organisation_group_id INTEGER,
    organisation_group_name TEXT
);

CREATE TABLE IF NOT EXISTS lms.cs_start_date_migration_extract (
    licence_ref TEXT,
    start_date TEXT
);

CREATE TABLE IF NOT EXISTS lms.cs_term_migration_extract (
    id INTEGER,
    licence_ref TEXT,
    term TEXT,
    years INTEGER,
    months INTEGER,
    days INTEGER
);
