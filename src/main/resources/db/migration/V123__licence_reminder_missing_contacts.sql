CREATE TABLE licence_reminder_missing_contacts(
    id UUID PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    responsible_organisation_id INTEGER NOT NULL,
    original_event_id UUID,
    reminder_type TEXT NOT NULL,
    deadline_date DATE NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT licence_reminder_missing_contacts_licence_fk FOREIGN KEY (licence_id) REFERENCES licences (id)
);

CREATE UNIQUE INDEX licence_reminder_missing_contacts_event_unique ON licence_reminder_missing_contacts(original_event_id, responsible_organisation_id, deadline_date) WHERE original_event_id IS NOT NULL;

CREATE UNIQUE INDEX licence_reminder_missing_contacts_licence_unique ON licence_reminder_missing_contacts(licence_id, reminder_type, responsible_organisation_id, deadline_date) WHERE original_event_id IS NULL;

CREATE INDEX licence_reminder_missing_contacts_licence_idx ON licence_reminder_missing_contacts(licence_id);

CREATE TABLE licence_reminder_missing_contacts_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_id INTEGER,
    responsible_organisation_id INTEGER,
    original_event_id UUID,
    reminder_type TEXT,
    deadline_date DATE,
    reported_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT licence_reminder_missing_contacts_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_reminder_missing_contacts_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_reminder_missing_contacts_aud_rev_idx ON licence_reminder_missing_contacts_aud (rev);
