# Employee Management System (EMS)

Full-stack Employee Management System built with React + Vite, Spring Boot, Spring Data JPA, MySQL, and JWT authentication.

## Main features

- Register and login with persistent employer accounts
- Registration validation requires a valid name/email and an 8-72 character password
- JWT-based authentication for protected API endpoints
- Employee CRUD with name, age, email, phone, department, designation, salary, joining date, and status
- Strong input validation on employee name, age, email, phone, department, designation, salary, joining date, leave balance, and status
- Joining-date validation: an employee cannot join in the future and cannot be moved after existing attendance records
- Employee mobile validation: exactly 10 digits starting with 6-9
- Database setup includes MySQL CHECK constraints for core ranges, statuses, and attendance/loan values
- Attendance saved in MySQL with one record per employee/day/month/year
- Attendance cannot be marked for inactive employees, before joining date, or for future dates
- Three-state attendance: Not Marked → Present → Absent → Not Marked
- Current-month payroll estimate with joining-date proration, attendance deduction, and loan EMI
- Employee loans stored in MySQL with amount/interest/tenure/outstanding validation, a combined active-EMI affordability cap of 50% of monthly salary, and a working payment action to reduce outstanding balance
- Company profile stored in MySQL
- Data survives browser refresh, frontend restart, and backend restart because business data is database-backed

## Technology

Frontend: React 19 + Vite

Backend: Spring Boot 3.2.3 + Spring Data JPA + Spring Security

Database: MySQL

Authentication: BCrypt password hashing + signed JWT (HS256)

## Default employer account

Email: `admin@ems.com`

Password: `admin123`

A newly registered employer account is saved in the `users` table and can be used again after restarting the application.

## Database

For a clean first-time installation, open MySQL Workbench and run `DATABASE_RESET.sql`, then run `DATABASE_SETUP.sql`. For an older EMS database, run `DATABASE_MIGRATE_EMPLOYER.sql` once to convert existing login roles.

For an existing EMS database, do NOT run the reset script unless you have a backup. Start the backend and let Hibernate update compatible schema changes. If an old `users` table contains a legacy `password` column from an earlier project version, remove that obsolete column or use the clean reset script.

## Start the backend

Open PowerShell in `backend`:

```powershell
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

## Start the frontend

Open a second PowerShell in the project root:

```powershell
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

The `.env.example` file documents the optional `VITE_API_BASE` variable for changing the backend URL.

## Important database checks

```sql
USE employee_db;
SELECT * FROM users;
SELECT * FROM employee;
SELECT * FROM attendance;
SELECT * FROM employee_loan;
SELECT * FROM company_profile;
```

## Assignment alignment

The project demonstrates React SPA integration with a Spring Boot REST API, JPA/MySQL persistence, CRUD operations, and JWT-protected API access.


### Attendance register and holiday rules

- The Attendance page now includes a monthly register for **all employees**. Each employee has one row and each date of the selected month appears as a column. A future working date such as October 26 is shown for every employee as `F` until that date arrives; the app does not create fake future attendance records.
- `P` = Present, `A` = Absent, `H` = Saturday/Sunday or public holiday, `F` = future, `B` = before joining, and `—` = not marked. The employee dropdown still lets the employer mark attendance for one employee at a time.
- Payroll's current-month estimate uses scheduled working days and excludes weekends and the listed holidays. Three recorded absences on working days remain allowed before the deduction begins.
- Moving public holidays are listed for Maharashtra for 2026, using the Maharashtra Government's public-holiday notification. Refresh these dates when the relevant government publishes the next year's list. Source: https://maharashtra.gov.in/site/Upload/pdf/Public-Holiday-2026.pdf and https://www.mmrda.maharashtra.gov.in/en/public-holidays
- Employee email addresses must end exactly in `@gmail.com`. This is checked in React and again by the backend.
- A loan's principal cannot exceed 50% of that employee's monthly salary, and the combined monthly EMI of all active loans also cannot exceed 50% of monthly salary. The backend enforces both checks, so bypassing the browser form does not bypass the rule.
- For an existing database, run `DATABASE_MIGRATION_ATTENDANCE_VIEW.sql` to add read-only `attendance_register`, `attendance_monthly_register`, and `employee_loan_register` views that display employee names alongside attendance and loan records in MySQL Workbench. The monthly view has a row for every employee/date and labels future dates instead of inserting fake attendance data. No database reset is required. `DATABASE_OPTIONAL_CLEANUP_FUTURE_ATTENDANCE.sql` lets you inspect future-dated rows such as an accidental October 26 record; its DELETE statement is commented out and must only be run after checking the results.
