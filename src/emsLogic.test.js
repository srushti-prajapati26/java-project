import test from "node:test";
import assert from "node:assert/strict";
import {
  calculateEMI,
  getAttendanceData,
  getSalaryData,
  getEmployeeEligibleRange,
  validateEmployeeInput,
  validateLoanInput,
  calculateTotalMonthlyEmi,
  getHolidayName,
  isWorkingDay,
  getWorkingDaysInMonth,
} from "./emsLogic.js";

const NOW = new Date(2026, 9, 8); // October 8, 2026 (local time)
const TODAY = "2026-10-08";

function employee(joining = "2026-10-01") {
  return { id: "E1234567890", name: "Test Employee", salary: 31000, joining };
}

function validEmployeeForm(overrides = {}) {
  return {
    name: "Prathamesh Gavankar",
    age: 20,
    email: "prathamesh.gavankar@gmail.com",
    phone: "9876543210",
    dept: "Management",
    designation: "HR Executive",
    salary: 50000,
    joining: TODAY,
    leaveBalance: 10,
    status: "Active",
    ...overrides,
  };
}

test("calculateEMI handles zero-interest loans", () => {
  assert.equal(calculateEMI(12000, 0, 12), 1000);
});

test("joining date limits attendance to employment period", () => {
  const stats = getAttendanceData(
    employee("2026-10-05"),
    { "E1234567890_5": "present", "E1234567890_6": "absent" },
    NOW
  );
  assert.equal(stats.scheduled, 4);
  assert.equal(stats.present, 1);
  assert.equal(stats.absent, 1);
  assert.equal(stats.unmarked, 2);
});

test("attendance before joining date is not counted", () => {
  const stats = getAttendanceData(
    employee("2026-10-05"),
    { "E1234567890_1": "present", "E1234567890_4": "absent" },
    NOW
  );
  assert.equal(stats.present, 0);
  assert.equal(stats.absent, 0);
  assert.equal(stats.scheduled, 4);
});

test("future joining date has no eligible days", () => {
  const range = getEmployeeEligibleRange(employee("2026-10-20"), NOW);
  assert.equal(range.scheduledDays, 0);
  const stats = getAttendanceData(employee("2026-10-20"), {}, NOW);
  assert.equal(stats.scheduled, 0);
});

test("unmarked working days do not become fake present days", () => {
  const stats = getAttendanceData(employee("2026-10-01"), {}, NOW);
  assert.equal(stats.present, 0);
  assert.equal(stats.absent, 0);
  // Oct 2 is Gandhi Jayanti and Oct 3-4 are the weekend.
  assert.equal(stats.scheduled, 5);
  assert.equal(stats.unmarked, 5);
});

test("payroll respects joining proration, three allowed absences, and loan EMI", () => {
  const stats = getSalaryData(
    employee("2026-10-05"),
    { "E1234567890_5": "absent", "E1234567890_6": "absent", "E1234567890_7": "absent", "E1234567890_8": "absent" },
    [{ employeeId: "E1234567890", amount: 10000, interest: 0, tenure: 10, outstanding: 10000 }],
    NOW
  );
  assert.equal(stats.scheduled, 4);
  // There are 20 scheduled working days in Maharashtra in October 2026.
  assert.equal(getWorkingDaysInMonth(2026, 10), 20);
  assert.equal(Math.round(stats.baseToDate), 6200);
  assert.equal(Math.round(stats.deduction), 1550);
  assert.equal(stats.emi, 1000);
  assert.equal(Math.round(stats.net), 3650);
});

test("employee validation requires an exactly 10-digit Indian mobile number", () => {
  assert.equal(validateEmployeeInput(validEmployeeForm({ phone: "987654321" }), TODAY), "Phone number must be exactly 10 digits and start with 6, 7, 8 or 9.");
  assert.equal(validateEmployeeInput(validEmployeeForm({ phone: "98765432101" }), TODAY), "Phone number must be exactly 10 digits and start with 6, 7, 8 or 9.");
  assert.equal(validateEmployeeInput(validEmployeeForm({ phone: "+919876543210" }), TODAY), "Phone number must be exactly 10 digits and start with 6, 7, 8 or 9.");
});

test("employee validation allows past and future joining dates and still validates age", () => {
  assert.equal(validateEmployeeInput(validEmployeeForm({ joining: "1999-01-01" }), TODAY), "");
  assert.equal(validateEmployeeInput(validEmployeeForm({ joining: "2035-12-31" }), TODAY), "");
  assert.equal(validateEmployeeInput(validEmployeeForm({ age: 17 }), TODAY), "Employee age must be between 18 and 100.");
});

test("employee validation rejects zero salary and invalid department", () => {
  assert.equal(validateEmployeeInput(validEmployeeForm({ salary: 0 }), TODAY), "Monthly salary must be greater than zero.");
  assert.equal(validateEmployeeInput(validEmployeeForm({ dept: "Unknown" }), TODAY), "Select a valid department.");
});


test("employee validation rejects malformed calendar dates", () => {
  assert.equal(validateEmployeeInput(validEmployeeForm({ joining: "2026-02-31" }), TODAY), "Enter a valid joining date.");
});

test("loan validation enforces positive amount and bounded term", () => {
  assert.equal(validateLoanInput({ amount: 0, interest: 8, tenure: 12 }), "Loan amount must be greater than zero.");
  assert.equal(validateLoanInput({ amount: 10000, interest: 101, tenure: 12 }), "Interest must be between 0 and 100%.");
  assert.equal(validateLoanInput({ amount: 10000, interest: 8, tenure: 121 }), "Tenure must be between 1 and 120 months.");
});

test("total active loan EMI is calculated per employee", () => {
  const loans = [
    { employeeId: "E1234567890", amount: 12000, interest: 0, tenure: 12, outstanding: 12000 },
    { employeeId: "E1234567890", amount: 24000, interest: 0, tenure: 12, outstanding: 0 },
    { employeeId: "OTHER", amount: 12000, interest: 0, tenure: 12, outstanding: 12000 },
  ];
  assert.equal(calculateTotalMonthlyEmi(loans, "E1234567890"), 1000);
});


test("employee email must end exactly in @gmail.com", () => {
  assert.equal(validateEmployeeInput(validEmployeeForm({ email: "prathamesh@gmail.com" }), TODAY), "");
  assert.match(validateEmployeeInput(validEmployeeForm({ email: "prathamesh@gmail.in" }), TODAY), /Gmail address ending exactly in @gmail\.com/);
  assert.match(validateEmployeeInput(validEmployeeForm({ email: "prathamesh@gmail.co.in" }), TODAY), /Gmail address ending exactly in @gmail\.com/);
  assert.match(validateEmployeeInput(validEmployeeForm({ email: "prathamesh@example.com" }), TODAY), /Gmail address ending exactly in @gmail\.com/);
});

test("weekends and Maharashtra public holidays are listed as non-working days", () => {
  assert.equal(getHolidayName("2026-10-03"), "Saturday");
  assert.equal(getHolidayName("2026-10-04"), "Sunday");
  assert.equal(getHolidayName("2026-10-02"), "Mahatma Gandhi Jayanti");
  assert.equal(getHolidayName("2026-10-20"), "Dasara");
  assert.equal(isWorkingDay("2026-10-03"), false);
  assert.equal(isWorkingDay("2026-10-02"), false);
  assert.equal(isWorkingDay("2026-10-26"), true);
});

test("loan principal cannot exceed half of monthly salary", () => {
  assert.equal(validateLoanInput({ amount: 25000, interest: 8, tenure: 12, maxLoanAmount: 25000 }), "");
  assert.match(validateLoanInput({ amount: 25000.01, interest: 8, tenure: 12, maxLoanAmount: 25000 }), /cannot exceed 50%/);
});
