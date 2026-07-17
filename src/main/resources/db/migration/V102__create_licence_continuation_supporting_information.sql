CREATE TABLE licence_continuation_supporting_information(
    id UUID PRIMARY KEY,
    licence_continuation_application_detail_id UUID,
    has_additional_supporting_information BOOLEAN,
    CONSTRAINT licence_continuation_supporting_information_fk FOREIGN KEY (licence_continuation_application_detail_id) REFERENCES licence_continuation_application_details (id)
);

CREATE INDEX licence_continuation_supporting_information_idx
    ON licence_continuation_supporting_information(licence_continuation_application_detail_id);

CREATE TABLE licence_continuation_supporting_information_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_continuation_application_detail_id UUID,
    has_additional_supporting_information BOOLEAN,
    CONSTRAINT licence_continuation_supporting_information_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_continuation_supporting_information_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_continuation_supporting_information_aud_rev_idx ON licence_continuation_supporting_information_aud (rev);
