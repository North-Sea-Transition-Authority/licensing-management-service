CREATE TABLE licence_transactions (
  id UUID PRIMARY KEY,
  regulator_reference TEXT NOT NULL
);

CREATE TABLE licence_positions (
  id UUID PRIMARY KEY,
  licence_id INTEGER NOT NULL,
  licence_transaction_id UUID NOT NULL,
  position_date DATE NOT NULL,
  position_date_order INTEGER NOT NULL,
  CONSTRAINT licence_positions_licence_fk FOREIGN KEY (licence_id) REFERENCES licences (id),
  CONSTRAINT licence_positions_licence_transaction_fk FOREIGN KEY (licence_transaction_id) REFERENCES licence_transactions (id),
  CONSTRAINT licence_positions_licence_transaction_date_uq UNIQUE (licence_id, licence_transaction_id, position_date),
  CONSTRAINT licence_positions_licence_date_order_uq UNIQUE (licence_id, position_date, position_date_order)
);

CREATE INDEX licence_positions_licence_idx ON licence_positions (licence_id);
CREATE INDEX licence_positions_transaction_idx ON licence_positions (licence_transaction_id);

CREATE TABLE licence_transactions_aud (
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  regulator_reference TEXT,
  CONSTRAINT licence_transactions_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_transactions_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_transactions_aud_rev_idx ON licence_transactions_aud (rev);

CREATE TABLE licence_positions_aud (
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  licence_id INTEGER,
  licence_transaction_id UUID,
  position_date DATE,
  position_date_order INTEGER,
  CONSTRAINT licence_positions_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_positions_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_positions_aud_rev_idx ON licence_positions_aud (rev);
