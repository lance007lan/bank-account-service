CREATE TABLE IF NOT EXISTS accounts (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    account_number   VARCHAR(20)  NOT NULL UNIQUE,
    customer_name    VARCHAR(255) NOT NULL,
    customer_nickname VARCHAR(255),
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_customer_name     ON accounts (LOWER(customer_name));
CREATE INDEX idx_accounts_customer_nickname ON accounts (LOWER(customer_nickname) text_pattern_ops);

CREATE TABLE offensive_words (
                                 id   BIGSERIAL    PRIMARY KEY,
                                 word VARCHAR(100) NOT NULL UNIQUE
);

-- Seed list — add or remove words via new migrations or directly in the DB
INSERT INTO offensive_words (word) VALUES
                                       ('damn'),
                                       ('hell'),
                                       ('crap'),
                                       ('bastard'),
                                       ('idiot'),
                                       ('stupid'),
                                       ('moron'),
                                       ('jerk');
