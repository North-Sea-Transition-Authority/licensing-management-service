CREATE TABLE licence_continuation_wpa_requirement_request(
    id UUID PRIMARY KEY,
    licence_continuation_application_detail_id UUID,
    work_programme_activities_completion_status Boolean,
    actions_to_complete_work_programme_activities TEXT,
    further_information TEXT,
    CONSTRAINT licence_continuation_wpa_requirement_request_fk FOREIGN KEY (licence_continuation_application_detail_id) REFERENCES licence_continuation_application_details (id)
);

CREATE INDEX licence_continuation_wpa_requirement_request_idx  ON licence_continuation_wpa_requirement_request(licence_continuation_application_detail_id);

CREATE TABLE licence_continuation_wpa_requirement_request_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_continuation_application_detail_id UUID,
    work_programme_activities_completion_status Boolean,
    actions_to_complete_work_programme_activities TEXT,
    further_information TEXT,
    CONSTRAINT licence_continuation_wpa_requirement_request_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_continuation_wpa_requirement_request_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_continuation_wpa_requirement_request_aud_rev_idx ON licence_continuation_wpa_requirement_request_aud (rev);