CREATE TABLE lms.cs_licence_migration_extract (
    licence_ref TEXT,
    licence_number TEXT,
    responsible_orgs TEXT,
    status TEXT,
    status_date TEXT
);

CREATE TABLE lms.cs_licence_org_mapping (
    cs_extract_responsible_organisation TEXT,
    organisation_unit_id INTEGER,
    organisation_group_id INTEGER,
    organisation_group_name TEXT
);

CREATE TABLE lms.cs_start_date_migration_extract (
    licence_ref TEXT,
    start_date TEXT
);

CREATE TABLE lms.cs_term_migration_extract (
    id INTEGER,
    licence_ref TEXT,
    term TEXT,
    years INTEGER,
    months INTEGER,
    days INTEGER,
    case_id TEXT,
    case_date TEXT
);

CREATE TABLE lms.cs_work_programme_migration_extract (
    id INTEGER,
    licence_ref TEXT,
    category TEXT,
    other_category TEXT,
    description TEXT,
    commitment TEXT,
    status TEXT,
    term TEXT,
    date_option TEXT,
    relative_years INTEGER,
    relative_months INTEGER,
    relative_days INTEGER,
    comments TEXT,
    unique_event_id UUID,
    case_id TEXT,
    case_date TEXT
);