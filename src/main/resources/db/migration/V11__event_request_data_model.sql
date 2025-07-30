CREATE TABLE schedule_work_programme_applications(
  id UUID PRIMARY KEY,
  licence_schedule_detail_id UUID,
  CONSTRAINT schedule_work_programme_applications_fk FOREIGN KEY (licence_schedule_detail_id) references licence_schedule_details (id)
);

CREATE INDEX schedule_work_programme_applications_idx ON schedule_work_programme_applications(licence_schedule_detail_id);

CREATE TABLE schedule_work_programme_applications_aud(
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  licence_schedule_detail_id UUID,
  CONSTRAINT schedule_work_programme_applications_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT schedule_work_programme_applications_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX schedule_work_programme_applications_aud_rev_idx ON schedule_work_programme_applications_aud (rev);

CREATE TABLE schedule_work_programme_application_details(
    id UUID PRIMARY KEY,
    schedule_work_programme_application_id UUID,
    version_number INTEGER,
    all_licensees_permission_confirmed BOOLEAN,
    CONSTRAINT schedule_work_programme_application_details_fk FOREIGN KEY (schedule_work_programme_application_id) references schedule_work_programme_applications (id)
);

CREATE INDEX schedule_work_programme_application_details_idx ON schedule_work_programme_application_details(schedule_work_programme_application_id);

CREATE TABLE schedule_work_programme_application_details_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_work_programme_application_id UUID,
    version_number INTEGER,
    all_licensees_permission_confirmed BOOLEAN,
    CONSTRAINT schedule_work_programme_application_details_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT schedule_work_programme_application_details_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX schedule_work_programme_application_details_aud_rev_idx ON schedule_work_programme_application_details_aud (rev);