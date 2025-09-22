ALTER TABLE licence_schedule_phases
ADD COLUMN licence_schedule_term_id UUID;

ALTER TABLE licence_schedule_phases
ADD CONSTRAINT licence_schedule_phase_term_fk
FOREIGN KEY (licence_schedule_term_id)
REFERENCES licence_schedule_terms(id);

ALTER TABLE licence_schedule_phases_aud
ADD COLUMN licence_schedule_term_id UUID;