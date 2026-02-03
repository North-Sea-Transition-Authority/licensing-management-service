CREATE TABLE licence_continuation_other_requirement_request(
    id UUID PRIMARY KEY,
    licence_continuation_application_detail_id UUID,
    financial_capacity_evidence_submission_status BOOLEAN,
    actions_to_provide_financial_evidence TEXT,
    CONSTRAINT licence_continuation_other_requirement_request_fk FOREIGN KEY (licence_continuation_application_detail_id) REFERENCES licence_continuation_application_details (id)
);

CREATE INDEX licence_continuation_other_requirement_request_idx
    ON licence_continuation_other_requirement_request(licence_continuation_application_detail_id);

CREATE TABLE licence_continuation_other_requirement_request_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_continuation_application_detail_id UUID,
    financial_capacity_evidence_submission_status BOOLEAN,
    actions_to_provide_financial_evidence TEXT,
    CONSTRAINT licence_continuation_other_requirement_request_aud_pkPRIMARY PRIMARY KEY (rev, id),
    CONSTRAINT licence_continuation_other_requirement_request_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_continuation_other_requirement_request_aud_rev_idx ON licence_continuation_other_requirement_request_aud (rev);