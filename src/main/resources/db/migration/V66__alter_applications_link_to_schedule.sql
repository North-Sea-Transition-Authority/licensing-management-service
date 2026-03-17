-- schedule_work_programme_applications:
ALTER TABLE schedule_work_programme_applications
  ADD COLUMN licence_schedule_id UUID,
  ADD COLUMN submitted_licence_schedule_detail_id UUID;

UPDATE schedule_work_programme_applications swpa
  SET licence_schedule_id = lsd.licence_schedule_id
  FROM licence_schedule_details lsd
  WHERE swpa.licence_schedule_detail_id = lsd.id;

UPDATE schedule_work_programme_applications swpa
  SET submitted_licence_schedule_detail_id = swpa.licence_schedule_detail_id
  FROM schedule_work_programme_application_details swpad
  WHERE swpad.schedule_work_programme_application_id = swpa.id
    AND swpad.status != 'DRAFT'
    AND swpad.version_number = (
      SELECT MAX(version_number)
      FROM schedule_work_programme_application_details swpad2
      WHERE swpad2.schedule_work_programme_application_id = swpa.id
    );

ALTER TABLE schedule_work_programme_applications
  ALTER COLUMN licence_schedule_id SET NOT NULL,
  ADD CONSTRAINT swpa_licence_schedule_fk FOREIGN KEY (licence_schedule_id) REFERENCES licence_schedules (id),
  ADD CONSTRAINT swpa_submitted_licence_schedule_detail_fk FOREIGN KEY (submitted_licence_schedule_detail_id) REFERENCES licence_schedule_details (id),
  DROP COLUMN licence_schedule_detail_id;

CREATE INDEX swpa_licence_schedule_idx ON schedule_work_programme_applications (licence_schedule_id);
CREATE INDEX swpa_submitted_licence_schedule_detail_idx ON schedule_work_programme_applications (submitted_licence_schedule_detail_id);

-- schedule_work_programme_applications_aud:
ALTER TABLE schedule_work_programme_applications_aud
  ADD COLUMN licence_schedule_id UUID,
  ADD COLUMN submitted_licence_schedule_detail_id UUID,
  DROP COLUMN licence_schedule_detail_id;

-- licence_continuation_applications:
ALTER TABLE licence_continuation_applications
  ADD COLUMN licence_schedule_id UUID,
  ADD COLUMN submitted_licence_schedule_detail_id UUID;

UPDATE licence_continuation_applications lca
  SET licence_schedule_id = lsd.licence_schedule_id
  FROM licence_schedule_details lsd
  WHERE lca.licence_schedule_detail_id = lsd.id;

UPDATE licence_continuation_applications lca
  SET submitted_licence_schedule_detail_id = lca.licence_schedule_detail_id
  FROM licence_continuation_application_details lcad
  WHERE lcad.licence_continuation_application_id = lca.id
    AND lcad.status != 'DRAFT'
    AND lcad.version_number = (
      SELECT MAX(version_number)
      FROM licence_continuation_application_details lcad2
      WHERE lcad2.licence_continuation_application_id = lcad.id
    );

ALTER TABLE licence_continuation_applications
  ALTER COLUMN licence_schedule_id SET NOT NULL,
  ADD CONSTRAINT lca_licence_schedule_fk FOREIGN KEY (licence_schedule_id) REFERENCES licence_schedules (id),
  ADD CONSTRAINT lca_submitted_licence_schedule_detail_fk FOREIGN KEY (submitted_licence_schedule_detail_id) REFERENCES licence_schedule_details (id),
  DROP COLUMN licence_schedule_detail_id;

CREATE INDEX lca_licence_schedule_idx ON licence_continuation_applications (licence_schedule_id);
CREATE INDEX lca_submitted_licence_schedule_detail_idx ON licence_continuation_applications (submitted_licence_schedule_detail_id);

-- licence_continuation_applications_aud:
ALTER TABLE licence_continuation_applications_aud
  ADD COLUMN licence_schedule_id UUID,
  ADD COLUMN submitted_licence_schedule_detail_id UUID,
  DROP COLUMN licence_schedule_detail_id;
