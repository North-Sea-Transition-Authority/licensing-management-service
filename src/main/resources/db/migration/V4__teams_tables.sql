CREATE TABLE teams (
  id UUID PRIMARY KEY,
  type TEXT NOT NULL,
  name TEXT NOT NULL,
  scope_type TEXT,
  scope_id TEXT
);

-- If the team is scoped, it's type+scopeType+scopeId must be unique
CREATE UNIQUE INDEX teams_scoped_unique ON teams (type, scope_type, scope_id) WHERE (scope_type IS NOT NULL);

-- If the teams is not scoped, it's type must be unique
CREATE UNIQUE INDEX teams_static_unique ON teams (type) WHERE (scope_type IS NULL);

CREATE TABLE teams_aud (
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  type TEXT,
  name TEXT,
  scope_type TEXT,
  scope_id TEXT,
  CONSTRAINT teams_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT teams_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX teams_aud_rev_idx ON teams_aud (rev);

CREATE TABLE team_roles (
  id UUID PRIMARY KEY,
  team_id UUID NOT NULL REFERENCES teams(id),
  role TEXT NOT NULL,
  wua_id BIGINT NOT NULL
);

CREATE INDEX team_roles_team_id_idx ON team_roles(team_id);

CREATE TABLE team_roles_aud (
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  team_id UUID,
  role TEXT,
  wua_id BIGINT,
  CONSTRAINT team_roles_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT team_roles_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX team_roles_aud_rev_idx ON team_roles_aud (rev);
