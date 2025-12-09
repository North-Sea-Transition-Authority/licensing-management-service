ALTER TABLE licence_schedule_rates
    ADD COLUMN relative_duration_days INTEGER;

ALTER TABLE licence_schedule_rates
    ADD COLUMN relative_duration_months INTEGER;

ALTER TABLE licence_schedule_rates
    ADD COLUMN relative_duration_years INTEGER;

ALTER TABLE licence_schedule_rates
    ADD COLUMN rate_relative_date_option TEXT;

ALTER TABLE licence_schedule_rates_aud
    ADD COLUMN relative_duration_days INTEGER;

ALTER TABLE licence_schedule_rates_aud
    ADD COLUMN relative_duration_months INTEGER;

ALTER TABLE licence_schedule_rates_aud
    ADD COLUMN relative_duration_years INTEGER;

ALTER TABLE licence_schedule_rates_aud
    ADD COLUMN rate_relative_date_option TEXT;