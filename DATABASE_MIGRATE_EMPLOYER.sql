-- Convert an older EMS login table to the current EMPLOYER role.
-- Back up employee_db first if you need to preserve the old users table.

USE employee_db;

-- Remove the old role CHECK only when it exists.
SET @drop_role_check = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE users DROP CHECK chk_user_role',
        'SELECT 1'
    )
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'users'
      AND constraint_name = 'chk_user_role'
);
PREPARE stmt FROM @drop_role_check;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Older EMS builds sometimes left a required legacy password column.
SET @drop_legacy_password = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE users DROP COLUMN password',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'password'
);
PREPARE stmt FROM @drop_legacy_password;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE users SET role = 'EMPLOYER';

ALTER TABLE users
    MODIFY role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYER';

ALTER TABLE users
    ADD CONSTRAINT chk_user_role CHECK (role = 'EMPLOYER');

SELECT id, name, email, role FROM users;
