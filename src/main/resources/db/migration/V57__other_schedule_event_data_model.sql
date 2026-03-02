CREATE TABLE other_schedule_events(
    id UUID PRIMARY KEY,
    licence_schedule_detail_id UUID NOT NULL,
    category TEXT,
    other_category_name TEXT,
    description TEXT,
    date_option TEXT,
    licence_schedule_term_id UUID,
    licence_schedule_phase_id UUID,
    relative_duration_days INTEGER,
    relative_duration_months INTEGER,
    relative_duration_years INTEGER,
    event_date DATE,
    comments TEXT,
    status TEXT,
    CONSTRAINT other_schedule_events_fk FOREIGN KEY (licence_schedule_detail_id) references licence_schedule_details (id),
    CONSTRAINT other_schedule_events_term_fk FOREIGN KEY (licence_schedule_term_id) references licence_schedule_terms (id),
    CONSTRAINT other_schedule_events_phase_fk FOREIGN KEY (licence_schedule_phase_id) references licence_schedule_phases (id)
);

CREATE INDEX other_schedule_events_idx ON other_schedule_events(licence_schedule_detail_id);

CREATE TABLE other_schedule_events_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_schedule_detail_id UUID,
    category TEXT,
    other_category_name TEXT,
    description TEXT,
    date_option TEXT,
    licence_schedule_term_id UUID,
    licence_schedule_phase_id UUID,
    relative_duration_days INTEGER,
    relative_duration_months INTEGER,
    relative_duration_years INTEGER,
    event_date DATE,
    comments TEXT,
    status TEXT,
    CONSTRAINT other_schedule_events_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT other_schedule_events_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX other_schedule_events_aud_rev_idx ON other_schedule_events_aud (rev);