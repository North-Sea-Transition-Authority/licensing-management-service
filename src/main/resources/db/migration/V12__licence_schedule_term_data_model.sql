CREATE TABLE licence_schedule_terms(
    id UUID PRIMARY KEY,
    licence_schedule_detail_id UUID,
    term_type TEXT,
    term_duration_days INTEGER,
    term_duration_months INTEGER,
    term_duration_years INTEGER,
    start_date DATE,
    end_date DATE,
    CONSTRAINT licence_schedule_terms_fk FOREIGN KEY (licence_schedule_detail_id) references licence_schedule_details (id)
);

CREATE INDEX licence_schedule_terms_idx ON licence_schedule_terms(licence_schedule_detail_id);

CREATE TABLE licence_schedule_terms_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_schedule_detail_id UUID,
    term_type TEXT,
    term_duration_days INTEGER,
    term_duration_months INTEGER,
    term_duration_years INTEGER,
    start_date DATE,
    end_date DATE,
    CONSTRAINT licence_schedule_terms_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_schedule_terms_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedule_terms_aud_rev_idx ON licence_schedule_terms_aud (rev);