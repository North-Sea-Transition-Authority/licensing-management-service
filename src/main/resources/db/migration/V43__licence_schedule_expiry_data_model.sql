CREATE TABLE licence_schedule_expiry_dates(
    id UUID PRIMARY KEY,
    licence_schedule_detail_id UUID NOT NULL,
    status TEXT,
    expiry_date DATE,
    comments TEXT,
    CONSTRAINT licence_schedule_expiry_dates_fk FOREIGN KEY (licence_schedule_detail_id) references licence_schedule_details (id)
);

CREATE INDEX licence_schedule_expiry_dates_idx ON licence_schedule_expiry_dates(licence_schedule_detail_id);

CREATE TABLE licence_schedule_expiry_dates_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_schedule_detail_id UUID,
    status TEXT,
    expiry_date DATE,
    comments TEXT,
    CONSTRAINT licence_schedule_expiry_dates_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_schedule_expiry_dates_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedule_expiry_dates_aud_rev_idx ON licence_schedule_expiry_dates_aud (rev);