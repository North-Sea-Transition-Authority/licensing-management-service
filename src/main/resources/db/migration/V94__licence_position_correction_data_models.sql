CREATE TABLE licence_position_corrections (
    id                         UUID PRIMARY KEY,
    licence_correction_id      UUID NOT NULL,
    change_type                TEXT NOT NULL,
    target_licence_position_id UUID,
    payload                    JSONB NOT NULL,
    CONSTRAINT licence_position_corrections_correction_fk
    FOREIGN KEY (licence_correction_id) REFERENCES licence_corrections (id),
    CONSTRAINT licence_position_corrections_target_position_fk
    FOREIGN KEY (target_licence_position_id) REFERENCES licence_positions (id)
);

CREATE INDEX licence_position_corrections_correction_idx
    ON licence_position_corrections (licence_correction_id);

CREATE INDEX licence_position_corrections_target_position_idx
    ON licence_position_corrections (target_licence_position_id);

CREATE TABLE licence_position_corrections_aud (
    rev                        SERIAL,
    revtype                    NUMERIC,
    id                         UUID,
    licence_correction_id      UUID,
    change_type                TEXT,
    target_licence_position_id UUID,
    payload                    JSONB,
    CONSTRAINT licence_position_corrections_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_position_corrections_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_position_corrections_aud_rev_idx ON licence_position_corrections_aud (rev);