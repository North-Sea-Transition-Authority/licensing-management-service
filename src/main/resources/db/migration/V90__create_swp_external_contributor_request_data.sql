CREATE TABLE swp_external_contributor_request(
    id UUID PRIMARY KEY,
    schedule_work_programme_application_id UUID,
    add_external_contributors Boolean,
    CONSTRAINT swp_external_contributor_request_fk FOREIGN KEY (schedule_work_programme_application_id) REFERENCES schedule_work_programme_applications (id)
);

CREATE INDEX swp_external_contributor_request_idx ON swp_external_contributor_request(schedule_work_programme_application_id);

CREATE TABLE swp_external_contributor_request_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_work_programme_application_id UUID,
    add_external_contributors Boolean,
    CONSTRAINT swp_external_contributor_request_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT swp_external_contributor_request_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX swp_external_contributor_request_aud_rev_idx ON swp_external_contributor_request_aud (rev);
