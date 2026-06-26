CREATE TABLE admin_accounts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'VIEWER',
    enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE admin_refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_account_id BIGINT       NOT NULL,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      DATETIME     NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_art_admin FOREIGN KEY (admin_account_id) REFERENCES admin_accounts (id) ON DELETE CASCADE
);

CREATE TABLE admin_audit_logs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id   BIGINT       NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    ip         VARCHAR(45)  NOT NULL,
    user_agent VARCHAR(500),
    action     VARCHAR(100) NOT NULL,
    target     VARCHAR(255),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_logs_admin_id ON admin_audit_logs (admin_id);
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs (created_at);
CREATE INDEX idx_admin_refresh_tokens_expires_at ON admin_refresh_tokens (expires_at);
