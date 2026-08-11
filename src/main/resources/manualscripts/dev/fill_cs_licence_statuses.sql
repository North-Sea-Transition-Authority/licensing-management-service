INSERT INTO licence_statuses(id, licence_id, status, status_date)
SELECT
    gen_random_uuid(),
    l.id,
    'EXTANT',
    '2026-01-01'
FROM licences l WHERE l.type = 'CARBON_STORAGE'