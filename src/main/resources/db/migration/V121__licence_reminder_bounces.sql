CREATE TABLE licence_reminder_bounces(
    id UUID PRIMARY KEY,
    notification_batch_reference UUID NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT licence_reminder_bounces_batch_unique UNIQUE (notification_batch_reference)
);

CREATE TABLE licence_reminder_bounces_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    notification_batch_reference UUID,
    reported_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT licence_reminder_bounces_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_reminder_bounces_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_reminder_bounces_aud_rev_idx ON licence_reminder_bounces_aud (rev);
