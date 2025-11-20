CREATE TABLE licence_schedule_rates(
    id UUID PRIMARY KEY,
    licence_schedule_detail_id UUID NOT NULL,
    rate_definition_option TEXT,
    licence_schedule_term_id UUID,
    licence_schedule_phase_id UUID,
    start_date DATE,
    rental_rate NUMERIC,
    comments TEXT,
    CONSTRAINT licence_schedule_rates_fk FOREIGN KEY (licence_schedule_detail_id) references licence_schedule_details (id),
    CONSTRAINT licence_schedule_rates_term_fk FOREIGN KEY (licence_schedule_term_id) references licence_schedule_terms (id),
    CONSTRAINT licence_schedule_rates_phase_fk FOREIGN KEY (licence_schedule_phase_id) references licence_schedule_phases (id)
);

CREATE INDEX licence_schedule_rates_idx ON licence_schedule_rates(licence_schedule_detail_id);

CREATE TABLE licence_schedule_rates_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_schedule_detail_id UUID,
    rate_definition_option TEXT,
    licence_schedule_term_id UUID,
    licence_schedule_phase_id UUID,
    start_date DATE,
    rental_rate NUMERIC,
    comments TEXT,
    CONSTRAINT licence_schedule_rates_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_schedule_rates_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedule_rates_aud_rev_idx ON licence_schedule_rates_aud (rev);