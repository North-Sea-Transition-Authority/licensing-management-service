-- Map AL, CE and DL licences to LANDWARD_PRODUCTION now those LicenceType values have been removed.
-- Run before the new LicenceType code deploys - Hibernate throws loading these licences otherwise.
-- The hourly PEARS refresh writes LANDWARD_PRODUCTION from then on. Safe to re-run.

UPDATE lms.licences
SET type = 'LANDWARD_PRODUCTION'
WHERE type IN ('AL', 'CE', 'DL');

UPDATE lms.licences_aud
SET type = 'LANDWARD_PRODUCTION'
WHERE type IN ('AL', 'CE', 'DL');
