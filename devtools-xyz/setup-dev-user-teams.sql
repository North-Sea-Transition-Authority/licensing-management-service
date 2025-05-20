DO $$
  DECLARE
    regulator_team_id uuid := gen_random_uuid();
    organisation_team_id uuid := gen_random_uuid();

  BEGIN
    INSERT INTO xyz.teams(id, type, name)
    VALUES(regulator_team_id, 'REGULATOR', 'Regulator');

    INSERT INTO xyz.team_roles(id, team_id, role, wua_id)
    VALUES(gen_random_uuid(), regulator_team_id, 'MANAGE_TEAM', 54673); -- template regulator admin

    INSERT INTO xyz.team_roles(id, team_id, role, wua_id)
    VALUES(gen_random_uuid(), regulator_team_id, 'CREATE_MANAGE_ANY_ORGANISATION_TEAM', 54673);

    INSERT INTO xyz.teams(id, type, name, scope_type, scope_id)
    VALUES (organisation_team_id, 'ORGANISATION', 'BP EXPLORATION', 'ORGANISATION', '50');

    INSERT INTO xyz.team_roles(id, team_id, role, wua_id)
    VALUES (gen_random_uuid(), organisation_team_id, 'EDIT_APPLICATION', 54672); -- template organisation editor user

END; $$