ALTER TABLE user_coin
    MODIFY COLUMN balance BIGINT NOT NULL DEFAULT 0;

ALTER TABLE coin_transaction_history
    MODIFY COLUMN transaction_type VARCHAR(64) NULL;

CREATE TABLE IF NOT EXISTS oxapay_recharge_order (
    id CHAR(36) NOT NULL,
    user_id INT NOT NULL,
    track_id VARCHAR(64) NULL,
    usd_amount DECIMAL(12, 2) NOT NULL,
    cny_amount DECIMAL(13, 3) NOT NULL,
    coin_amount INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_url VARCHAR(512) NULL,
    expired_at DATETIME NULL,
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oxapay_recharge_track_id (track_id),
    KEY idx_oxapay_recharge_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
