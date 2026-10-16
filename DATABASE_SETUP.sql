-- Employee Management System - MySQL setup
-- This script is safe for a fresh database. It does NOT delete existing data.
-- Spring Boot also keeps the schema synchronized with JPA using ddl-auto=update.

CREATE DATABASE IF NOT EXISTS employee_db;
USE employee_db;

CREATE TABLE IF NOT EXISTS employee (
    employee_id VARCHAR(50) PRIMARY KEY,
    employee_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    department VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    base_salary DOUBLE NOT NULL DEFAULT 0,
    days_present INT NOT NULL DEFAULT 0,
    leave_balance INT NOT NULL DEFAULT 10,
    loan_amount DOUBLE NOT NULL DEFAULT 0,
    daily_attendance_status VARCHAR(30) NOT NULL DEFAULT 'NOT MARKED',
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    joining_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    CONSTRAINT chk_employee_id CHECK (employee_id REGEXP '^E[0-9]{10,16}$'),
    CONSTRAINT chk_employee_age CHECK (age BETWEEN 18 AND 100),
    CONSTRAINT chk_employee_salary CHECK (base_salary > 0),
    CONSTRAINT chk_employee_days_present CHECK (days_present BETWEEN 0 AND 31),
    CONSTRAINT chk_employee_leave_balance CHECK (leave_balance BETWEEN 0 AND 365),
    CONSTRAINT chk_employee_loan_amount CHECK (loan_amount >= 0),
    CONSTRAINT chk_employee_attendance_status CHECK (daily_attendance_status IN ('NOT MARKED', 'PRESENT', 'ABSENT')),
    CONSTRAINT chk_employee_phone CHECK (phone REGEXP '^[6-9][0-9]{9}$'),
    CONSTRAINT chk_employee_status CHECK (status IN ('Active', 'Inactive'))
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(500) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYER',
    CONSTRAINT chk_user_role CHECK (role = 'EMPLOYER')
);

CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    day INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_attendance_day CHECK (day BETWEEN 1 AND 31),
    CONSTRAINT chk_attendance_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_attendance_year CHECK (year BETWEEN 2000 AND 2100),
    CONSTRAINT chk_attendance_status CHECK (status IN ('present', 'absent')),
    UNIQUE KEY uq_attendance (employee_id, day, month, year),
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS employee_loan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    amount DOUBLE NOT NULL,
    interest DOUBLE NOT NULL,
    tenure INT NOT NULL,
    outstanding DOUBLE NOT NULL,
    CONSTRAINT chk_loan_amount CHECK (amount > 0),
    CONSTRAINT chk_loan_interest CHECK (interest BETWEEN 0 AND 100),
    CONSTRAINT chk_loan_tenure CHECK (tenure BETWEEN 1 AND 120),
    CONSTRAINT chk_loan_outstanding CHECK (outstanding BETWEEN 0 AND amount),
    CONSTRAINT fk_loan_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS company_profile (
    id BIGINT PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    system_name VARCHAR(150) NOT NULL,
    CONSTRAINT chk_company_name_length CHECK (CHAR_LENGTH(company_name) BETWEEN 2 AND 150),
    CONSTRAINT chk_system_name_length CHECK (CHAR_LENGTH(system_name) BETWEEN 2 AND 150)
);

INSERT INTO company_profile (id, company_name, email, phone, system_name)
VALUES (1, 'EMS', 'admin@ems.com', '+91 98765 43210', 'Employee Management System')
ON DUPLICATE KEY UPDATE id = id;

CREATE OR REPLACE VIEW attendance_register AS
SELECT
    a.id,
    e.employee_id,
    e.employee_name,
    STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d') AS attendance_date,
    a.day,
    a.month,
    a.year,
    UPPER(a.status) AS status
FROM attendance a
INNER JOIN employee e ON e.employee_id = a.employee_id
WHERE STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d') <= CURDATE()
  AND STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d') >= e.joining_date
  AND WEEKDAY(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) < 5
  AND NOT (
      (MONTH(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) = 1 AND DAY(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) = 26)
      OR (MONTH(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) = 5 AND DAY(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) = 1)
      OR (MONTH(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) = 8 AND DAY(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) = 15)
      OR (MONTH(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) = 10 AND DAY(STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d')) = 2)
      OR (a.year = 2026 AND CONCAT(LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')) IN (
          '02-15', '02-19', '03-03', '03-19', '03-21', '03-26', '03-31',
          '04-03', '04-14', '05-28', '06-26', '08-26', '09-14', '10-20', '11-08', '11-10',
          '11-24', '12-25'
      ))
  );

-- Use this view when you want attendance rows with names visible in Workbench:
SELECT employee_id, employee_name, attendance_date, status
FROM attendance_register
ORDER BY attendance_date, employee_name;


-- Current-month register: one row per employee and calendar day, including all employees' future dates.
-- Future/unmarked dates are displayed as labels in this VIEW; no fake attendance record is inserted.
CREATE OR REPLACE VIEW attendance_monthly_register AS
SELECT
    e.employee_id,
    e.employee_name,
    calendar.work_date AS attendance_date,
    DAY(calendar.work_date) AS day,
    MONTH(calendar.work_date) AS month,
    YEAR(calendar.work_date) AS year,
    CASE
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '01-26' THEN 'HOLIDAY: Republic Day'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '02-15' THEN 'HOLIDAY: Mahashivratri'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '02-19' THEN 'HOLIDAY: Chhatrapati Shivaji Maharaj Jayanti'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '03-03' THEN 'HOLIDAY: Holi (Second Day)'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '03-19' THEN 'HOLIDAY: Gudhi Padwa'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '03-21' THEN 'HOLIDAY: Ramzan-Id'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '03-26' THEN 'HOLIDAY: Ram Navami'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '03-31' THEN 'HOLIDAY: Mahavir Janmakalyanak'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '04-03' THEN 'HOLIDAY: Good Friday'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '04-14' THEN 'HOLIDAY: Dr. Babasaheb Ambedkar Jayanti'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '05-01' THEN 'HOLIDAY: Maharashtra Day / Buddha Pournima'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '05-28' THEN 'HOLIDAY: Bakri Id'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '06-26' THEN 'HOLIDAY: Moharum'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '08-15' THEN 'HOLIDAY: Independence Day / Parsi New Year'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '08-26' THEN 'HOLIDAY: Id-E-Milad'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '09-14' THEN 'HOLIDAY: Ganesh Chaturthi'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '10-02' THEN 'HOLIDAY: Mahatma Gandhi Jayanti'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '10-20' THEN 'HOLIDAY: Dasara'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '11-08' THEN 'HOLIDAY: Diwali Amavasya (Laxmi Pujan)'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '11-10' THEN 'HOLIDAY: Diwali (Bali Pratipada)'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '11-24' THEN 'HOLIDAY: Guru Nanak Jayanti'
        WHEN YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') = '12-25' THEN 'HOLIDAY: Christmas Day'
        WHEN DATE_FORMAT(calendar.work_date, '%m-%d') = '01-26' THEN 'HOLIDAY: Republic Day'
        WHEN DATE_FORMAT(calendar.work_date, '%m-%d') = '05-01' THEN 'HOLIDAY: Maharashtra Day'
        WHEN DATE_FORMAT(calendar.work_date, '%m-%d') = '08-15' THEN 'HOLIDAY: Independence Day'
        WHEN DATE_FORMAT(calendar.work_date, '%m-%d') = '10-02' THEN 'HOLIDAY: Mahatma Gandhi Jayanti'
        WHEN WEEKDAY(calendar.work_date) = 5 THEN 'HOLIDAY: Saturday'
        WHEN WEEKDAY(calendar.work_date) = 6 THEN 'HOLIDAY: Sunday'
        WHEN calendar.work_date < e.joining_date THEN 'BEFORE JOINING'
        WHEN calendar.work_date > CURDATE() THEN 'FUTURE'
        WHEN a.status IS NULL THEN 'NOT MARKED'
        ELSE UPPER(a.status)
    END AS status
FROM employee e
CROSS JOIN (
    SELECT day_number AS day,
           DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (DAYOFMONTH(CURDATE()) - 1) DAY), INTERVAL (day_number - 1) DAY) AS work_date
    FROM (
        SELECT 1 AS day_number UNION ALL
        SELECT 2 UNION ALL
        SELECT 3 UNION ALL
        SELECT 4 UNION ALL
        SELECT 5 UNION ALL
        SELECT 6 UNION ALL
        SELECT 7 UNION ALL
        SELECT 8 UNION ALL
        SELECT 9 UNION ALL
        SELECT 10 UNION ALL
        SELECT 11 UNION ALL
        SELECT 12 UNION ALL
        SELECT 13 UNION ALL
        SELECT 14 UNION ALL
        SELECT 15 UNION ALL
        SELECT 16 UNION ALL
        SELECT 17 UNION ALL
        SELECT 18 UNION ALL
        SELECT 19 UNION ALL
        SELECT 20 UNION ALL
        SELECT 21 UNION ALL
        SELECT 22 UNION ALL
        SELECT 23 UNION ALL
        SELECT 24 UNION ALL
        SELECT 25 UNION ALL
        SELECT 26 UNION ALL
        SELECT 27 UNION ALL
        SELECT 28 UNION ALL
        SELECT 29 UNION ALL
        SELECT 30 UNION ALL
        SELECT 31
    ) day_numbers
    WHERE day_number <= DAY(LAST_DAY(CURDATE()))
) calendar
LEFT JOIN attendance a
    ON a.employee_id = e.employee_id
   AND a.day = calendar.day
   AND a.month = MONTH(CURDATE())
   AND a.year = YEAR(CURDATE())
   AND calendar.work_date <= CURDATE()
   AND calendar.work_date >= e.joining_date
   AND WEEKDAY(calendar.work_date) < 5
   AND NOT (
       DATE_FORMAT(calendar.work_date, '%m-%d') IN ('01-26', '05-01', '08-15', '10-02')
       OR (YEAR(calendar.work_date) = 2026 AND DATE_FORMAT(calendar.work_date, '%m-%d') IN (
           '02-15', '02-19', '03-03', '03-19', '03-21', '03-26', '03-31',
           '04-03', '04-14', '05-28', '06-26', '08-26', '09-14', '10-20',
           '11-08', '11-10', '11-24', '12-25'
       ))
   );

-- To see every employee and every date in the current month (including the 26th):
SELECT employee_id, employee_name, attendance_date, status
FROM attendance_monthly_register
ORDER BY employee_name, attendance_date;


CREATE OR REPLACE VIEW employee_loan_register AS
SELECT
    l.id,
    e.employee_id,
    e.employee_name,
    l.amount,
    l.interest,
    l.tenure,
    CASE
        WHEN l.interest = 0 THEN l.amount / l.tenure
        ELSE (l.amount * (l.interest / 12.0 / 100.0) * POW(1 + (l.interest / 12.0 / 100.0), l.tenure))
             / (POW(1 + (l.interest / 12.0 / 100.0), l.tenure) - 1)
    END AS monthly_emi,
    l.outstanding,
    CASE WHEN l.outstanding > 0 THEN 'ACTIVE' ELSE 'PAID' END AS loan_status
FROM employee_loan l
INNER JOIN employee e ON e.employee_id = l.employee_id;

-- Loan records with employee names visible in Workbench:
SELECT employee_id, employee_name, amount, interest, tenure, monthly_emi, outstanding, loan_status
FROM employee_loan_register
ORDER BY employee_name, id;

SHOW FULL TABLES;
