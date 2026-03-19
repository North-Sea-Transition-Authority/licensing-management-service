ALTER TABLE schedule_work_programme_applications
  ADD CONSTRAINT swpa_application_reference_unique UNIQUE (application_reference);

ALTER TABLE licence_continuation_applications
  ADD CONSTRAINT lca_application_reference_unique UNIQUE (application_reference);
