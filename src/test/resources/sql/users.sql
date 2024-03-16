CREATE TABLE users (
    email text NOT NULL,
    hashedPassword text NOT NULL,
    firstName text,
    lastName text,
    company text,
    role text NOT NULL
);

ALTER TABLE users ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users(
    email,
    hashedPassword,
    firstName,
    lastName,
    company,
    role
) VALUES (
    'i@gmail.com',
    'pwd',
    'Imran',
    'Sarwar',
    'IMG',
    'ADMIN'
);

INSERT INTO users(
    email,
    hashedPassword,
    firstName,
    lastName,
    company,
    role
) VALUES (
    'i2@gmail.com',
    'pwd2',
    'Imran',
    'Sarwar',
    'IMG',
    'RECRUITER'
);