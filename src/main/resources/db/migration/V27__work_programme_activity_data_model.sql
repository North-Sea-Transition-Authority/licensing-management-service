CREATE TABLE work_programme_activities(
    id UUID PRIMARY KEY,
    licence_schedule_detail_id UUID NOT NULL,
    category TEXT,
    other_category_name TEXT,
    description TEXT,
    commitment TEXT,
    date_option TEXT,
    licence_schedule_term_id UUID,
    licence_schedule_phase_id UUID,
    due_date DATE,
    comments TEXT,
    CONSTRAINT work_programme_activities_fk FOREIGN KEY (licence_schedule_detail_id) references licence_schedule_details (id),
    CONSTRAINT work_programme_activities_term_fk FOREIGN KEY (licence_schedule_term_id) references licence_schedule_terms (id),
    CONSTRAINT work_programme_activities_phase_fk FOREIGN KEY (licence_schedule_phase_id) references licence_schedule_phases (id)
);

CREATE INDEX work_programme_activities_idx ON work_programme_activities(licence_schedule_detail_id);

CREATE TABLE work_programme_activities_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_schedule_detail_id UUID,
    category TEXT,
    other_category_name TEXT,
    description TEXT,
    commitment TEXT,
    date_option TEXT,
    licence_schedule_term_id UUID,
    licence_schedule_phase_id UUID,
    due_date DATE,
    comments TEXT,
    CONSTRAINT work_programme_activities_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT work_programme_activities_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX work_programme_activities_aud_rev_idx ON work_programme_activities_aud (rev);