CREATE TABLE document_templates_metadata (
  id UUID PRIMARY KEY
, document_template_id UUID NOT NULL
, licence_type TEXT NOT NULL
, application_type TEXT NOT NULL
);

CREATE TABLE document_templates_metadata_aud (
  rev SERIAL
, revtype NUMERIC
, id UUID
, document_template_id UUID
, licence_type TEXT
, application_type TEXT
, PRIMARY KEY (rev, id)
, FOREIGN KEY (rev) REFERENCES audit_revisions(rev)
);