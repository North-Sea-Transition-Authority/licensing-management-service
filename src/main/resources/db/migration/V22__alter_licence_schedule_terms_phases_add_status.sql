ALTER TABLE licence_schedule_terms
    ADD COLUMN status TEXT;

ALTER TABLE licence_schedule_terms_aud
    ADD COLUMN status TEXT;

ALTER TABLE licence_schedule_phases
    ADD COLUMN status TEXT;

ALTER TABLE licence_schedule_phases_aud
    ADD COLUMN status TEXT;