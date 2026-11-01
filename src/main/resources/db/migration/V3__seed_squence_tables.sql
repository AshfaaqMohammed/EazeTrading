-- V3__seed_sequence_tables.sql
-- Hibernate @GeneratedValue(AUTO) uses table sequences (*_seq) holding a single
-- next_val row. A freshly Flyway-built schema (e.g. the test DB) has these tables
-- EMPTY, causing "Could not read a hi value, populate the table: <x>_seq".
-- Seed each with a starting row, but ONLY if empty, so the already-populated
-- production DB is left untouched (it must keep exactly one row per seq table).

INSERT INTO `asset_seq` (`next_val`)              SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `asset_seq`);
INSERT INTO `order_book_seq` (`next_val`)         SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `order_book_seq`);
INSERT INTO `order_item_seq` (`next_val`)         SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `order_item_seq`);
INSERT INTO `payment_details_seq` (`next_val`)    SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `payment_details_seq`);
INSERT INTO `payment_order_seq` (`next_val`)      SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `payment_order_seq`);
INSERT INTO `user_seq` (`next_val`)               SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `user_seq`);
INSERT INTO `verification_code_seq` (`next_val`)  SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `verification_code_seq`);
INSERT INTO `wallet_seq` (`next_val`)             SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wallet_seq`);
INSERT INTO `wallet_transaction_seq` (`next_val`) SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wallet_transaction_seq`);
INSERT INTO `watch_list_seq` (`next_val`)         SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `watch_list_seq`);
INSERT INTO `withdrawal_seq` (`next_val`)         SELECT 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `withdrawal_seq`);