CREATE TABLE licence_correction_owned_gis_artefacts (
  id                    UUID PRIMARY KEY,
  licence_correction_id UUID NOT NULL,
  artefact_type         TEXT NOT NULL,
  artefact_id           UUID NOT NULL,
  CONSTRAINT licence_correction_owned_gis_artefacts_correction_fk FOREIGN KEY (licence_correction_id) REFERENCES licence_corrections (id),
  CONSTRAINT licence_correction_owned_gis_artefacts_uk UNIQUE (licence_correction_id, artefact_type, artefact_id)
);

CREATE INDEX licence_correction_owned_gis_artefacts_correction_idx ON licence_correction_owned_gis_artefacts (licence_correction_id);

CREATE TABLE licence_correction_owned_gis_artefacts_aud (
  rev                   SERIAL,
  revtype               NUMERIC,
  id                    UUID,
  licence_correction_id UUID,
  artefact_type         TEXT,
  artefact_id           UUID,
  CONSTRAINT licence_correction_owned_gis_artefacts_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_correction_owned_gis_artefacts_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_correction_owned_gis_artefacts_aud_rev_idx ON licence_correction_owned_gis_artefacts_aud (rev);
