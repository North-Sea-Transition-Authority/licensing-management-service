-- Create the user
CREATE USER datawarehouseuser WITH PASSWORD '${datawarehouse-user-password}';

-- Give them access to the schema
GRANT USAGE ON SCHEMA ${application-schema} TO datawarehouseuser;

-- Allow them to select from any table
GRANT SELECT ON ALL TABLES IN SCHEMA ${application-schema} TO datawarehouseuser;

-- Make select the default privilege level for datawarehouseuser for any future tables,
-- so this migration doesn't need to be re-run when new tables are added
ALTER DEFAULT PRIVILEGES IN SCHEMA ${application-schema} GRANT SELECT ON TABLES TO datawarehouseuser;