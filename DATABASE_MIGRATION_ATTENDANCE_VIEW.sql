-- Optional, non-destructive migration for an existing employee_db.
-- It adds a read-only view that shows employee names with their attendance rows.
-- Existing tables and records are not changed.

USE employee_db;

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

-- Focused check: shows the 26th for every employee. Until that date arrives,
-- a working day is labelled FUTURE rather than stored as a fake attendance record.
SELECT employee_id, employee_name, attendance_date, status
FROM attendance_monthly_register
WHERE day = 26
ORDER BY employee_name;

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
