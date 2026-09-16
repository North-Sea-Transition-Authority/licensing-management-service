DROP INDEX licence_reminders_event_unique;

DROP INDEX licence_reminders_activity_unique;

CREATE UNIQUE INDEX licence_reminders_event_unique ON licence_reminders(original_event_id, responsible_organisation_id, notice_period, deadline_date) WHERE original_event_id IS NOT NULL;
