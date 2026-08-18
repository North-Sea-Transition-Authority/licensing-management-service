CREATE TABLE IF NOT EXISTS lms.pears_contacts_migration_extract (
    organisation_group_id INTEGER NOT NULL,
    wua_id INTEGER NOT NULL,
    PRIMARY KEY (organisation_group_id, wua_id)
);
