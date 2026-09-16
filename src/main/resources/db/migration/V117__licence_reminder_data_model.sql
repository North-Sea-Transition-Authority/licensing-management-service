CREATE TABLE licence_reminders(
    id UUID PRIMARY KEY,
    schedule_event_id UUID NOT NULL,
    original_event_id UUID NOT NULL,
    licence_id INTEGER NOT NULL,
    responsible_organisation_id INTEGER NOT NULL,
    deadline_date DATE NOT NULL,
    notice_period TEXT NOT NULL,
    notification_batch_reference UUID NOT NULL,
    queued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT licence_reminders_schedule_event_fk FOREIGN KEY (schedule_event_id) REFERENCES schedule_events (id),
    CONSTRAINT licence_reminders_licence_fk FOREIGN KEY (licence_id) REFERENCES licences (id),
    CONSTRAINT licence_reminders_unique UNIQUE (original_event_id, responsible_organisation_id, notice_period, deadline_date)
);

CREATE INDEX licence_reminders_batch_idx ON licence_reminders(notification_batch_reference);

CREATE INDEX licence_reminders_licence_idx ON licence_reminders(licence_id);

CREATE TABLE licence_reminders_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_event_id UUID,
    original_event_id UUID,
    licence_id INTEGER,
    responsible_organisation_id INTEGER,
    deadline_date DATE,
    notice_period TEXT,
    notification_batch_reference UUID,
    queued_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT licence_reminders_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_reminders_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_reminders_aud_rev_idx ON licence_reminders_aud (rev);
