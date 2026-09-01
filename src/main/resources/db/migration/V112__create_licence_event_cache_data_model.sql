CREATE TABLE licence_event_cache(
    id UUID PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    licence_reference TEXT,
    original_event_id UUID,
    event_type TEXT,
    current_term_phase TEXT,
    next_term_phase TEXT,
    activity_type TEXT,
    event_date DATE,
    quad_block TEXT,
    steward_wua_id BIGINT,
    application_id UUID,
    application_type TEXT,
    CONSTRAINT licence_event_cache_licence_fk FOREIGN KEY (licence_id) REFERENCES licences (id)
);

CREATE INDEX licence_event_cache_licence_idx ON licence_event_cache(licence_id);

CREATE INDEX licence_event_cache_event_date_idx ON licence_event_cache(event_date);

CREATE TABLE licence_event_cache_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_id INTEGER,
    licence_reference TEXT,
    original_event_id UUID,
    event_type TEXT,
    current_term_phase TEXT,
    next_term_phase TEXT,
    activity_type TEXT,
    event_date DATE,
    quad_block TEXT,
    steward_wua_id BIGINT,
    application_id UUID,
    application_type TEXT,
    CONSTRAINT licence_event_cache_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_event_cache_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_event_cache_aud_rev_idx ON licence_event_cache_aud (rev);
