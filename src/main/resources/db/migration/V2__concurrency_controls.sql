-- 1. Optimistic locking column on 'asset' (JPA @Version)
-- so the remain valid under ddl-auto=validate and start optimistic tracking
ALTER TABLE `asset` ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;

-- Defense in depth: even if applicaiton-level locking is bypassed by a bug,
-- MySQL enforces this and rolls back any write that would make the balance negative
ALTER TABLE `wallet` ADD CONSTRAINT `chk_wallet_balance_non_negative` CHECK (`balance` >= 0);