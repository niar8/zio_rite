CREATE TABLE IF NOT EXISTS users(
    id BIGSERIAL PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    hashed_password TEXT NOT NULL
);