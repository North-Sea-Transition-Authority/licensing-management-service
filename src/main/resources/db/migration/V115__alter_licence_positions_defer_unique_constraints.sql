CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE licence_positions
DROP CONSTRAINT licence_positions_licence_date_order_uq;

ALTER TABLE licence_positions
    ADD CONSTRAINT licence_positions_licence_date_order_uq
        EXCLUDE (licence_id WITH =, position_date WITH =, position_date_order WITH =)
        WHERE (status <> 'REMOVED') DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE licence_positions
DROP CONSTRAINT licence_positions_licence_transaction_date_uq;

ALTER TABLE licence_positions
    ADD CONSTRAINT licence_positions_licence_transaction_date_uq
        EXCLUDE (licence_id WITH =, licence_transaction_id WITH =, position_date WITH =)
        WHERE (status <> 'REMOVED') DEFERRABLE INITIALLY DEFERRED;
