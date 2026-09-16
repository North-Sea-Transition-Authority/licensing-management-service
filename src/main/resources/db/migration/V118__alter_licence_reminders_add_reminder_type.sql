ALTER TABLE licence_reminders ALTER COLUMN schedule_event_id DROP NOT NULL;

ALTER TABLE licence_reminders ALTER COLUMN original_event_id DROP NOT NULL;

ALTER TABLE licence_reminders ADD COLUMN reminder_type TEXT;

UPDATE licence_reminders SET reminder_type = 'TERM_OR_PHASE_END';

ALTER TABLE licence_reminders ALTER COLUMN reminder_type SET NOT NULL;

ALTER TABLE licence_reminders DROP CONSTRAINT licence_reminders_unique;

CREATE UNIQUE INDEX licence_reminders_event_unique ON licence_reminders(original_event_id, responsible_organisation_id, notice_period, deadline_date) WHERE reminder_type = 'TERM_OR_PHASE_END';

CREATE UNIQUE INDEX licence_reminders_licence_unique ON licence_reminders(licence_id, reminder_type, responsible_organisation_id, notice_period, deadline_date) WHERE reminder_type = 'LICENCE_EXPIRY';

ALTER TABLE licence_reminders_aud ADD COLUMN reminder_type TEXT;
