-- ─────────────────────────────────────────────────────────────
-- Create database
-- ─────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS auth_db        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS job_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS application_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS profile_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- Create user
-- ─────────────────────────────────────────────────────────────
CREATE USER IF NOT EXISTS 'jobboard'@'%' IDENTIFIED BY 'jobboard_password';

GRANT ALL PRIVILEGES ON auth_db.*        TO 'jobboard'@'%';
GRANT ALL PRIVILEGES ON job_db.*         TO 'jobboard'@'%';
GRANT ALL PRIVILEGES ON application_db.* TO 'jobboard'@'%';
GRANT ALL PRIVILEGES ON profile_db.*     TO 'jobboard'@'%';

FLUSH PRIVILEGES;
