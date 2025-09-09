CREATE TABLE licence_schedule_phases(
    id UUID PRIMARY KEY,
    licence_schedule_detail_id UUID NOT NULL,
    phase_type TEXT NOT NULL,
    phase_duration_days INTEGER NOT NULL,
    phase_duration_months INTEGER NOT NULL,
    phase_duration_years INTEGER NOT NULL,
    start_date DATE,
    end_date DATE,
    comments TEXT,
    CONSTRAINT licence_schedule_phases_fk FOREIGN KEY (licence_schedule_detail_id) references licence_schedule_details (id)
);

CREATE INDEX licence_schedule_phases_idx ON licence_schedule_phases(licence_schedule_detail_id);

CREATE TABLE licence_schedule_phases_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_schedule_detail_id UUID,
    phase_type TEXT,
    phase_duration_days INTEGER,
    phase_duration_months INTEGER,
    phase_duration_years INTEGER,
    start_date DATE,
    end_date DATE,
    comments TEXT,
    CONSTRAINT licence_schedule_phases_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_schedule_phases_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedule_phases_aud_rev_idx ON licence_schedule_phases_aud (rev);