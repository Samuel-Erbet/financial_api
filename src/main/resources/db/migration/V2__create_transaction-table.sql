CREATE TABLE IF NOT EXISTS `transaction` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    user_id BIGINT,
    type VARCHAR(50) NOT NULL,
    created_at DATE DEFAULT (CURRENT_DATE),
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE SET NULL
);
