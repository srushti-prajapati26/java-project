# EMS - Quick Run Instructions

## 1. MySQL

Make sure MySQL Server is running.

For a completely fresh database:

1. Run `DATABASE_RESET.sql`.
2. Run `DATABASE_SETUP.sql`.

For your existing database (recommended; preserves data):

1. **Do not run** `DATABASE_RESET.sql`.
2. Run `DATABASE_MIGRATION_ATTENDANCE_VIEW.sql` once. It creates read-only views to display employee names with attendance/loan details and to show every employee for every date of the current month, including the 26th.
3. To inspect any future-dated attendance records already stored, run `DATABASE_OPTIONAL_CLEANUP_FUTURE_ATTENDANCE.sql`. Review its SELECT output. Its DELETE is commented out, so nothing will be removed unless you deliberately uncomment and execute it.

The local default database user is `root` with password `12344`. The backend also supports environment variables `DB_USERNAME` and `DB_PASSWORD`.

## 2. Backend

Open PowerShell:

```powershell
cd backend
mvn spring-boot:run
```

Wait for Spring Boot to report that it started on port `8080`.

## 3. Frontend

Open a second PowerShell:

```powershell
npm install
npm run dev
```

Open the displayed Vite URL, normally:

`http://localhost:5173`

## 4. Employer login / register

Default employer:

- Email: `admin@ems.com`
- Password: `admin123`

Or use Register to create a permanent database-backed account.

## 5. Recommended test sequence

1. Register a new account.
2. Logout.
3. Login again using the same email/password.
4. Add an employee with an age of 25 and a joining date of today or earlier.
5. Confirm the employee row appears in MySQL.
6. Try a 9-digit phone number and an 11-digit phone number; both must be rejected.
7. Try age 17 or salary 0; both must be rejected.
8. Try a future joining date; it must be rejected.
9. Open Attendance. The monthly register shows every employee as a row and every date (including the 26th) as a column; future dates and before-joining dates cannot be marked.
10. Confirm Saturdays, Sundays, and listed Maharashtra public holidays show as `H` and cannot be marked. The payroll working-day count excludes those dates.
11. Mark Present and Absent on eligible working dates; confirm rows appear in MySQL.
12. Open Salary & Payroll and confirm the selected employee changes when the dropdown changes.
13. Create a loan for an active employee. Loan principal cannot exceed 50% of monthly salary, and combined monthly EMI must also stay within 50% of monthly salary. Confirm the loan appears in MySQL and affects payroll.
14. Use Pay on a loan and confirm its outstanding balance decreases in MySQL.
15. Mark an employee Inactive and confirm attendance and new-loan actions are blocked.
16. Refresh the browser; records must remain.
17. Restart Spring Boot; records must remain.

## Useful SQL views for an existing database

After running `DATABASE_MIGRATION_ATTENDANCE_VIEW.sql`:

```sql
USE employee_db;
SELECT * FROM attendance_register ORDER BY attendance_date, employee_name;
SELECT * FROM attendance_monthly_register ORDER BY employee_name, attendance_date;
SELECT employee_id, employee_name, attendance_date, status
FROM attendance_monthly_register WHERE day = 26 ORDER BY employee_name;
SELECT * FROM employee_loan_register ORDER BY employee_name, id;
```

The monthly attendance view shows October 26 for all employees as `FUTURE` until that day arrives. Attendance rows are only saved after a real mark is made; do not insert future Present/Absent rows just to make the date appear.

## Current API endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/employees`
- `POST /api/employees`
- `PUT /api/employees/{id}`
- `DELETE /api/employees/{id}`
- `GET /api/attendance?month={month}&year={year}`
- `POST /api/attendance`
- `DELETE /api/attendance?employeeId={id}&day={day}&month={month}&year={year}`
- `GET /api/loans`
- `POST /api/loans`
- `POST /api/loans/{id}/payment?amount={amount}`
- `PUT /api/loans/{id}`
- `DELETE /api/loans/{id}`
- `GET /api/company-profile`
- `PUT /api/company-profile`
