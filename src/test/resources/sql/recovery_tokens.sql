CREATE TABLE recovery_tokens (
    email text NOT NULL,
    token text NOT NULL,
    expiration bigint NOT NULL
);

ALTER TABLE recovery_tokens ADD CONSTRAINT pk_recovery_tokens PRIMARY KEY (email);