-- OPTIONAL: inspect attendance records dated after today.
-- Do not run a DELETE until you confirm these are accidental/pre-marked future records.
USE employee_db;

SELECT a.*, e.employee_name,
       STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d') AS attendance_date
FROM attendance a
LEFT JOIN employee e ON e.employee_id = a.employee_id
WHERE STR_TO_DATE(CONCAT(a.year, '-', LPAD(a.month, 2, '0'), '-', LPAD(a.day, 2, '0')), '%Y-%m-%d') > CURDATE()
ORDER BY attendance_date, e.employee_name;

-- After reviewing the SELECT results, uncomment the next statement ONLY if the future rows are invalid.
-- DELETE FROM attendance
-- WHERE STR_TO_DATE(CONCAT(year, '-', LPAD(month, 2, '0'), '-', LPAD(day, 2, '0')), '%Y-%m-%d') > CURDATE();
