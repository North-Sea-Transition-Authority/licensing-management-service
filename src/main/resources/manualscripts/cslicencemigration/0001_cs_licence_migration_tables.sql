CREATE TABLE lms.cs_licence_migration_extract (
    licence_ref TEXT,
    prefix TEXT,
    licence_number TEXT,
    responsible_orgs TEXT
);

CREATE TABLE lms.cs_licence_org_mapping (
    cs_extract_responsible_organisation TEXT,
    organisation_unit_id INTEGER,
    organisation_group_id INTEGER,
    organisation_group_name TEXT
);