SELECT t.orgs
FROM (
    SELECT unnest(string_to_array(e.responsible_orgs, ',')) orgs
    FROM lms.cs_licence_migration_extract e
     ) as t
GROUP BY t.orgs
ORDER BY 1;