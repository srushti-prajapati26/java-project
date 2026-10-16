# Final EMS Logical-Test Report

## Fixed issues

- Employee age is no longer hard-coded; it is entered by the user and validated from 18 to 100.
- Employee email, phone, joining date, status, salary, department, designation, and leave balance are sent to and read from Spring Boot/MySQL.
- Future joining dates are rejected in both React and Spring Boot.
- Attendance is stored in MySQL instead of browser localStorage.
- Loans are stored in MySQL instead of browser localStorage.
- Attendance before joining date, attendance on future dates, weekends, and listed Maharashtra public holidays are rejected; old invalid rows are filtered out of the active attendance view.
- Unmarked attendance is no longer incorrectly counted as Present.
- Attendance selection and payroll selection use the actual employee ID, so switching employees changes the displayed data correctly. The attendance page also includes a monthly matrix with every employee as a row and every calendar date as a column.
- Attendance uses the real number of days in the current month.
- Payroll uses the employee's joining date and current-month-to-date employment period.
- Three absent days are allowed before an attendance salary deduction is applied, matching the original EMS rule.
- Loan EMI is applied only to the selected employee's active loan records. Loan principal cannot exceed 50% of monthly salary, and combined active EMI is also capped at 50% of monthly salary.
- Loan records can be deleted and are removed from payroll immediately.
- Deleting an employee also deletes the employee's attendance and loan records.
- Protected business APIs now require a valid JWT.
- New passwords are stored using BCrypt. The project can also upgrade compatible passwords created by the previous SHA-256 EMS build after a successful login.
- JWT expiration and secret are configurable through environment variables.
- Database username, password, frontend origin, and API base URL are configurable without changing source code.

## Checks completed in this validation environment

- React/JSX source static review: completed
- Frontend attendance/payroll/business-rule tests: 15/15 PASS
- Additional backend Mockito tests were added for invalid employee data, joining-date consistency, inactive attendance, combined loan EMI, and loan ownership; Maven was not available here to execute them.
- Java source static review: completed; a full Maven compile/test was not available in this validation environment.
- Project structure and frontend/backend API route consistency: PASS by static inspection

## Checks not fully executed here

A complete `npm run build` could not be executed because the validation container does not have the Vite/React build packages installed and the npm install did not complete. A complete Maven test/build could not be executed because Maven and the Spring/JJWT dependency cache are not available in this validation container. The project is packaged with its normal `package-lock.json` and Maven `pom.xml`; install the dependencies on the development machine before building.


## Logical validation checks added

- Employee phone: exactly 10 digits, starting with 6-9; the React field also blocks non-digit and 11+ digit entry.
- Employee age: 18-100.
- Monthly salary: greater than zero.
- Joining date: today or earlier.
- Leave balance: 0-365 days.
- Department/status: restricted to the values exposed by the UI.
- Attendance: active employees only, never before joining date or in the future.
- Loans: valid positive amount, 0-100% interest, 1-120 month tenure, outstanding between 0 and amount, combined active EMI capped at 50% of monthly salary, and loan ownership cannot be transferred.
- Company profile and account registration fields also receive server-side validation.
- Database setup scripts include MySQL CHECK constraints for core employee, attendance, loan, role, and profile ranges/status values.
- Loan aggregate outstanding is synchronized back to the employee record after loan create/update/delete.


## Additional checks for the October 2026 attendance/loan update

- JSX syntax transpilation: 0 syntax diagnostics.
- Standalone `WorkCalendar.java` compile: passed.
- Frontend tests now pass 15/15, including exact `@gmail.com` email validation, weekend/public holiday rules, and the 50% loan-principal cap.
- Existing-database view migration is non-destructive. It adds `attendance_register`, `attendance_monthly_register`, and `employee_loan_register`; no employee, attendance, or loan rows are inserted/deleted by the migration.
- Full Maven tests and Vite production build are not claimed as passed: Maven is not installed in the validation environment, and frontend dependencies were not installed here.
