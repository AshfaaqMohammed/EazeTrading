-- V1__baseline.sql
-- Baseline schema for EazeTrading, captured from the existing MySQL database.
-- Faithful structural snapshot (no data). Adopted by Flyway via baseline-on-migrate.
-- NOTE: id generation uses Hibernate table sequences (*_seq) because entities use
-- @GeneratedValue(strategy = AUTO). These *_seq tables are part of the schema.
-- Tables are ordered so FK-referenced parents are created before their children.

-- ---------------------------------------------------------------------------
-- Parent / referenced tables
-- ---------------------------------------------------------------------------

CREATE TABLE `user` (
                        `id` bigint NOT NULL,
                        `email` varchar(255) DEFAULT NULL,
                        `full_name` varchar(255) DEFAULT NULL,
                        `password` varchar(255) DEFAULT NULL,
                        `role` enum('ROLE_ADMIN','ROLE_CUSTOMER') DEFAULT NULL,
                        `is_enabled` bit(1) DEFAULT NULL,
                        `send_to` enum('EMAIL','MOBILE') DEFAULT NULL,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `coin` (
                        `id` varchar(255) NOT NULL,
                        `ath` double DEFAULT NULL,
                        `ath_change_percentage` double DEFAULT NULL,
                        `ath_date` datetime(6) DEFAULT NULL,
                        `atl` double DEFAULT NULL,
                        `atl_change_percentage` double DEFAULT NULL,
                        `atl_date` datetime(6) DEFAULT NULL,
                        `circulating_supply` bigint DEFAULT NULL,
                        `current_price` double DEFAULT NULL,
                        `fully_diluted_valuation` bigint DEFAULT NULL,
                        `high24h` double DEFAULT NULL,
                        `image` varchar(255) DEFAULT NULL,
                        `last_updated` datetime(6) DEFAULT NULL,
                        `low24h` double DEFAULT NULL,
                        `market_cap` bigint DEFAULT NULL,
                        `market_cap_change24h` bigint DEFAULT NULL,
                        `market_cap_change_percentage24h` double DEFAULT NULL,
                        `market_cap_rank` int DEFAULT NULL,
                        `max_supply` bigint DEFAULT NULL,
                        `name` varchar(255) DEFAULT NULL,
                        `price_change24h` double DEFAULT NULL,
                        `price_change_percentage24h` double DEFAULT NULL,
                        `roi` varchar(255) DEFAULT NULL,
                        `symbol` varchar(255) DEFAULT NULL,
                        `total_supply` bigint DEFAULT NULL,
                        `total_volume` bigint DEFAULT NULL,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `wallet` (
                          `id` bigint NOT NULL,
                          `balance` decimal(38,2) DEFAULT NULL,
                          `user_id` bigint DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `UKhgee4p1hiwadqinr0avxlq4eb` (`user_id`),
                          CONSTRAINT `FKbs4ogwiknsup4rpw8d47qw9dx` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `watch_list` (
                              `id` bigint NOT NULL,
                              `user_id` bigint DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `UKjloavm7g1immn099tcwtxsh9w` (`user_id`),
                              CONSTRAINT `FKbjgki17qu7m9jeynp3jqxrhue` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_book` (
                              `id` bigint NOT NULL,
                              `order_status` enum('CANCELLED','ERROR','FILED','PARTIALLY_FILLED','PENDING','SUCCESS') NOT NULL,
                              `order_type` enum('BUY','SELL') NOT NULL,
                              `price` decimal(38,2) NOT NULL,
                              `timestamp` datetime(6) DEFAULT NULL,
                              `user_id` bigint DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              KEY `FKakg6dn0hqdie9e3tx3x2pkon2` (`user_id`),
                              CONSTRAINT `FKakg6dn0hqdie9e3tx3x2pkon2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Child tables (reference the parents above)
-- ---------------------------------------------------------------------------

CREATE TABLE `asset` (
                         `id` bigint NOT NULL,
                         `buy_price` decimal(38,2) DEFAULT NULL,
                         `quantity` decimal(38,2) DEFAULT NULL,
                         `coin_id` varchar(255) DEFAULT NULL,
                         `user_id` bigint DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `FK4fbggwnyleunvc740bpomxadq` (`coin_id`),
                         KEY `FKi2t8rfq8blfbh1rpvbxqrmgvd` (`user_id`),
                         CONSTRAINT `FK4fbggwnyleunvc740bpomxadq` FOREIGN KEY (`coin_id`) REFERENCES `coin` (`id`),
                         CONSTRAINT `FKi2t8rfq8blfbh1rpvbxqrmgvd` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_item` (
                              `id` bigint NOT NULL,
                              `buy_price` decimal(38,2) DEFAULT NULL,
                              `quantity` decimal(38,2) DEFAULT NULL,
                              `sell_price` decimal(38,2) DEFAULT NULL,
                              `coin_id` varchar(255) DEFAULT NULL,
                              `order_id` bigint DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `UK5gjhq2fmknk50h8859nf0bcmx` (`order_id`),
                              KEY `FKig6jwdbdicfc5jqjcebh9kay6` (`coin_id`),
                              CONSTRAINT `FKidcua8oj3rs3g5vwgfw8schhh` FOREIGN KEY (`order_id`) REFERENCES `order_book` (`id`),
                              CONSTRAINT `FKig6jwdbdicfc5jqjcebh9kay6` FOREIGN KEY (`coin_id`) REFERENCES `coin` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_details` (
                                   `id` bigint NOT NULL,
                                   `account_holder_name` varchar(255) DEFAULT NULL,
                                   `account_number` varchar(255) DEFAULT NULL,
                                   `bank_name` varchar(255) DEFAULT NULL,
                                   `ifsc` varchar(255) DEFAULT NULL,
                                   `user_id` bigint DEFAULT NULL,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `UKi9gdmffq8xom3cbj5c6ix57ix` (`user_id`),
                                   CONSTRAINT `FKl8p3c4lta5278p86b9cjjju8q` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_order` (
                                 `id` bigint NOT NULL,
                                 `amount` decimal(38,2) DEFAULT NULL,
                                 `payment_method` enum('RAZORPAY','STRIPE') DEFAULT NULL,
                                 `status` enum('FAILED','PENDING','SUCCESS') DEFAULT NULL,
                                 `user_id` bigint DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `FKdkj084gg4ak2uy5183lo7qu3q` (`user_id`),
                                 CONSTRAINT `FKdkj084gg4ak2uy5183lo7qu3q` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `forgot_password_token` (
                                         `id` varchar(255) NOT NULL,
                                         `otp` varchar(255) DEFAULT NULL,
                                         `send_to` varchar(255) DEFAULT NULL,
                                         `verification_type` enum('EMAIL','MOBILE') DEFAULT NULL,
                                         `user_id` bigint DEFAULT NULL,
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `UK9s1okjmlql64mh55l9ehjlkmh` (`user_id`),
                                         CONSTRAINT `FKifnfhlpdkra9ckg3ag756hxwq` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `two_factorotp` (
                                 `id` varchar(255) NOT NULL,
                                 `jwt` varchar(255) DEFAULT NULL,
                                 `otp` varchar(255) DEFAULT NULL,
                                 `user_id` bigint DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `UKpsadp5pvvn15wr0aqye79pax8` (`user_id`),
                                 CONSTRAINT `FKlv6rhhjdnxxhfqv0se3f90wl5` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `verification_code` (
                                     `id` bigint NOT NULL,
                                     `email` varchar(255) DEFAULT NULL,
                                     `mobile` varchar(255) DEFAULT NULL,
                                     `otp` varchar(255) DEFAULT NULL,
                                     `verification_type` enum('EMAIL','MOBILE') DEFAULT NULL,
                                     `user_id` bigint DEFAULT NULL,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `UKn576esytmxxfkgon3ja83h5vp` (`user_id`),
                                     CONSTRAINT `FKgy5dhio3a6c9me7s0x9v1y4d2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `wallet_transaction` (
                                      `id` bigint NOT NULL,
                                      `amount` decimal(38,2) DEFAULT NULL,
                                      `date` datetime(6) DEFAULT NULL,
                                      `purpose` varchar(255) DEFAULT NULL,
                                      `transfer_id` varchar(255) DEFAULT NULL,
                                      `type` enum('ADD_MONEY','BUY_ASSET','SELL_ASSET','WALLET_TRANSFER','WITHDRAWAL') DEFAULT NULL,
                                      `wallet_id` bigint DEFAULT NULL,
                                      PRIMARY KEY (`id`),
                                      KEY `FK6cnvafp3a0xhbs0eh9w26sett` (`wallet_id`),
                                      CONSTRAINT `FK6cnvafp3a0xhbs0eh9w26sett` FOREIGN KEY (`wallet_id`) REFERENCES `wallet` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `withdrawal` (
                              `id` bigint NOT NULL,
                              `amount` decimal(38,2) DEFAULT NULL,
                              `date` datetime(6) DEFAULT NULL,
                              `status` enum('DECLINE','PENDING','SUCCESS') DEFAULT NULL,
                              `user_id` bigint DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              KEY `FK55stck4rmqxuavcy08f9d5jv1` (`user_id`),
                              CONSTRAINT `FK55stck4rmqxuavcy08f9d5jv1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `watch_list_coins` (
                                    `watch_list_id` bigint NOT NULL,
                                    `coins_id` varchar(255) NOT NULL,
                                    KEY `FK97jgrq6nlp46nl88b1e54j4q3` (`coins_id`),
                                    KEY `FKiktvrphafua2il78qpxni7h58` (`watch_list_id`),
                                    CONSTRAINT `FK97jgrq6nlp46nl88b1e54j4q3` FOREIGN KEY (`coins_id`) REFERENCES `coin` (`id`),
                                    CONSTRAINT `FKiktvrphafua2il78qpxni7h58` FOREIGN KEY (`watch_list_id`) REFERENCES `watch_list` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Hibernate id-generation sequence tables (@GeneratedValue AUTO -> table sequences)
-- ---------------------------------------------------------------------------

CREATE TABLE `asset_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `order_book_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `order_item_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `payment_details_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `payment_order_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `user_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `verification_code_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `wallet_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `wallet_transaction_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `watch_list_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `withdrawal_seq` ( `next_val` bigint DEFAULT NULL ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;