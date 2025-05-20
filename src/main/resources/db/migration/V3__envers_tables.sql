CREATE TABLE audit_revisions (
  rev SERIAL PRIMARY KEY,
  created_date_time TIMESTAMPTZ,
  user_wua_id BIGINT,
  proxy_user_wua_id BIGINT
);
