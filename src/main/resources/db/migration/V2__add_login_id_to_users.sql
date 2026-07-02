-- This migration assumes MySQL 8 and utf8mb4_0900_as_cs collation support.
-- If prodlike/production DB does not support this collation, review utf8mb4_bin as a fallback.
-- Phase 9 login_id allows only English letters and digits. Korean, special characters,
-- and whitespace are rejected by application validation.
-- Phase 9 nickname policy is 2-12 characters, but the DB column remains VARCHAR(30)
-- to protect existing operational data. New nickname length validation is applied later
-- at the DTO/API validation layer.
-- If existing users data contains duplicate nicknames, adding uk_users_nickname will fail.
-- Check duplicate nicknames in prodlike before applying this migration in production.
--
-- Preflight reference SQL, do not execute from this migration:
-- SHOW COLLATION LIKE 'utf8mb4_0900_as_cs';
--
-- SELECT nickname COLLATE utf8mb4_0900_as_cs AS nickname_key, COUNT(*) AS cnt
-- FROM users
-- GROUP BY nickname COLLATE utf8mb4_0900_as_cs
-- HAVING COUNT(*) > 1;
--
-- SELECT generated_login_id, COUNT(*) AS cnt
-- FROM (
--     SELECT CONCAT(
--         'u',
--         LPAD(
--             CAST(id AS CHAR),
--             GREATEST(3, CHAR_LENGTH(CAST(id AS CHAR))),
--             '0'
--         )
--     ) AS generated_login_id
--     FROM users
-- ) generated
-- GROUP BY generated_login_id
-- HAVING COUNT(*) > 1;
--
-- SELECT id, generated_login_id
-- FROM (
--     SELECT
--         id,
--         CONCAT(
--             'u',
--             LPAD(
--                 CAST(id AS CHAR),
--                 GREATEST(3, CHAR_LENGTH(CAST(id AS CHAR))),
--                 '0'
--             )
--         ) AS generated_login_id
--     FROM users
-- ) generated
-- WHERE CHAR_LENGTH(generated_login_id) > 12;

ALTER TABLE users
    ADD COLUMN login_id VARCHAR(12)
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_as_cs
    NULL;

UPDATE users
SET login_id = CONCAT(
    'u',
    LPAD(
        CAST(id AS CHAR),
        GREATEST(3, CHAR_LENGTH(CAST(id AS CHAR))),
        '0'
    )
)
WHERE login_id IS NULL;

ALTER TABLE users
    MODIFY login_id VARCHAR(12)
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_as_cs
    NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_login_id UNIQUE (login_id);

ALTER TABLE users
    MODIFY nickname VARCHAR(30)
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_as_cs
    NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
