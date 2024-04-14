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
    '$2a$10$FqgaSbLZ5MEPQvD5qDTV5e0Xz/N8q3oT027ZwtLDsSIhMoQaMFGjC',
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
    '$2a$10$OLVVnNYAlUwFmjpSCoYnWeEkkeJBMQcNcKXR01m.gK.01McgpzudO',
    'Imran',
    'Sarwar',
    'IMG',
    'RECRUITER'
);