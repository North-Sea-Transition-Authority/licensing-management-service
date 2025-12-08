ALTER TABLE work_programme_activities
    ADD COLUMN relative_duration_days INTEGER;

ALTER TABLE work_programme_activities
    ADD COLUMN relative_duration_months INTEGER;

ALTER TABLE work_programme_activities
    ADD COLUMN relative_duration_years INTEGER;

ALTER TABLE work_programme_activities_aud
    ADD COLUMN relative_duration_days INTEGER;

ALTER TABLE work_programme_activities_aud
    ADD COLUMN relative_duration_months INTEGER;

ALTER TABLE work_programme_activities_aud
    ADD COLUMN relative_duration_years INTEGER;