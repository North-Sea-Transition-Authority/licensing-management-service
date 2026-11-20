-- RD (retention and development area) and PA (pre-producing area) events, copied as they are from each migrated PEARS
-- schedule version
CREATE TABLE licence_schedule_area_events (
  id UUID PRIMARY KEY,
  licence_schedule_detail_id UUID NOT NULL,
  event_mnem TEXT NOT NULL,
  subtype TEXT,
  event_date DATE,
  comments TEXT,
  area_name TEXT,
  area_short_name TEXT,
  area_type TEXT,
  area_version TEXT,
  pears_event_id TEXT,
  CONSTRAINT licence_schedule_area_events_detail_fk FOREIGN KEY (licence_schedule_detail_id)
    REFERENCES licence_schedule_details (id)
);

CREATE INDEX licence_schedule_area_events_detail_idx ON licence_schedule_area_events (licence_schedule_detail_id);

CREATE TABLE licence_schedule_area_events_aud (
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  licence_schedule_detail_id UUID,
  event_mnem TEXT,
  subtype TEXT,
  event_date DATE,
  comments TEXT,
  area_name TEXT,
  area_short_name TEXT,
  area_type TEXT,
  area_version TEXT,
  pears_event_id TEXT,
  CONSTRAINT licence_schedule_area_events_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_schedule_area_events_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedule_area_events_aud_rev_idx ON licence_schedule_area_events_aud (rev);
