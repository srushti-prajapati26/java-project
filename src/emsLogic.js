export const DEPARTMENTS = [
  "Management",
  "Technology",
  "Finance",
  "Human Resources",
  "Sales",
  "Operations",
];

export const STATUS_OPTIONS = ["Active", "Inactive"];

const NAME_PATTERN = /^[A-Za-z][A-Za-z .'-]{1,99}$/;
const DESIGNATION_PATTERN = /^[A-Za-z0-9][A-Za-z0-9 &()./'-]{1,99}$/;
// EMS employee email policy: Gmail addresses ending exactly in .com.
const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@gmail\.com$/i;
const PHONE_PATTERN = /^[6-9][0-9]{9}$/;

// Maharashtra public holidays for 2026, based on the published state holiday list.
// Moving/religious holiday dates must be refreshed when the relevant government publishes a new year's list.
const MAHARASHTRA_HOLIDAYS_2026 = Object.freeze({
  "2026-01-26": "Republic Day",
  "2026-02-15": "Mahashivratri",
  "2026-02-19": "Chhatrapati Shivaji Maharaj Jayanti",
  "2026-03-03": "Holi (Second Day)",
  "2026-03-19": "Gudhi Padwa",
  "2026-03-21": "Ramzan-Id",
  "2026-03-26": "Ram Navami",
  "2026-03-31": "Mahavir Janmakalyanak",
  "2026-04-03": "Good Friday",
  "2026-04-14": "Dr. Babasaheb Ambedkar Jayanti",
  "2026-05-01": "Maharashtra Day / Buddha Pournima",
  "2026-05-28": "Bakri Id",
  "2026-06-26": "Moharum",
  "2026-08-15": "Independence Day / Parsi New Year",
  "2026-08-26": "Id-E-Milad",
  "2026-09-14": "Ganesh Chaturthi",
  "2026-10-02": "Mahatma Gandhi Jayanti",
  "2026-10-20": "Dasara",
  "2026-11-08": "Diwali Amavasya (Laxmi Pujan)",
  "2026-11-10": "Diwali (Bali Pratipada)",
  "2026-11-24": "Guru Nanak Jayanti",
  "2026-12-25": "Christmas Day",
});

const ANNUAL_HOLIDAYS = Object.freeze({
  "01-26": "Republic Day",
  "05-01": "Maharashtra Day",
  "08-15": "Independence Day",
  "10-02": "Mahatma Gandhi Jayanti",
});

export function getTodayInputValue(now = new Date()) {
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

export function parseLocalDate(value) {
  if (!value) return null;
  const [year, month, day] = String(value).split("-").map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function normalizeDate(value) {
  if (value instanceof Date) {
    if (Number.isNaN(value.getTime())) return null;
    return new Date(value.getFullYear(), value.getMonth(), value.getDate());
  }
  const date = parseLocalDate(value);
  if (!date || getTodayInputValue(date) !== String(value)) return null;
  return date;
}

export function validateEmployeeInput(form, today = getTodayInputValue()) {
  const name = String(form?.name ?? "").trim();
  const age = Number(form?.age);
  const email = String(form?.email ?? "").trim();
  const phone = String(form?.phone ?? "").trim();
  const designation = String(form?.designation ?? "").trim();
  const salary = Number(form?.salary);
  const joining = String(form?.joining ?? "");
  const leaveBalance = Number(form?.leaveBalance);
  const department = String(form?.dept ?? "");
  const status = String(form?.status ?? "");

  if (!name) return "Employee name is required.";
  if (name.length < 2 || name.length > 100 || !NAME_PATTERN.test(name)) {
    return "Name must be 2-100 characters and contain letters, spaces, apostrophes, periods or hyphens only.";
  }
  if (!Number.isInteger(age) || age < 18 || age > 100) return "Employee age must be between 18 and 100.";
  if (!email || email.length > 150 || !EMAIL_PATTERN.test(email)) {
    return "Employee email must be a Gmail address ending exactly in @gmail.com.";
  }
  if (!PHONE_PATTERN.test(phone)) return "Phone number must be exactly 10 digits and start with 6, 7, 8 or 9.";
  if (!DEPARTMENTS.includes(department)) return "Select a valid department.";
  if (!designation || designation.length < 2 || designation.length > 100 || !DESIGNATION_PATTERN.test(designation)) {
    return "Designation must be 2-100 valid characters.";
  }
  if (!Number.isFinite(salary) || salary <= 0) return "Monthly salary must be greater than zero.";
  if (!joining) return "Joining date is required.";
  const joiningDate = parseLocalDate(joining);
  if (!joiningDate || getTodayInputValue(joiningDate) !== joining) return "Enter a valid joining date.";
  if (!Number.isInteger(leaveBalance) || leaveBalance < 0 || leaveBalance > 365) {
    return "Leave balance must be between 0 and 365 days.";
  }
  if (!STATUS_OPTIONS.includes(status)) return "Employee status must be Active or Inactive.";
  return "";
}

export function getHolidayName(value) {
  const date = normalizeDate(value);
  if (!date) return "";
  const key = getTodayInputValue(date);
  const specificHoliday = MAHARASHTRA_HOLIDAYS_2026[key];
  if (specificHoliday) return specificHoliday;

  const monthDay = `${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  const annualHoliday = ANNUAL_HOLIDAYS[monthDay];
  if (annualHoliday) return annualHoliday;

  if (date.getDay() === 0) return "Sunday";
  if (date.getDay() === 6) return "Saturday";
  return "";
}

export function isWorkingDay(value) {
  return getHolidayName(value) === "";
}

export function getWorkingDaysInRange(startValue, endValue) {
  const start = normalizeDate(startValue);
  const end = normalizeDate(endValue);
  if (!start || !end || start > end) return 0;

  let count = 0;
  const cursor = new Date(start.getFullYear(), start.getMonth(), start.getDate());
  while (cursor <= end) {
    if (isWorkingDay(cursor)) count += 1;
    cursor.setDate(cursor.getDate() + 1);
  }
  return count;
}

export function getWorkingDaysInMonth(year, month) {
  const first = new Date(year, month - 1, 1);
  const last = new Date(year, month - 1, getDaysInMonth(year, month));
  return getWorkingDaysInRange(first, last);
}

export function getDaysInMonth(year, month) {
  return new Date(year, month, 0).getDate();
}

export function getEmployeeEligibleRange(employee, now = new Date()) {
  if (!employee?.joining) return { startDay: 1, endDay: 0, scheduledDays: 0 };
  const joining = normalizeDate(employee.joining);
  if (!joining) return { startDay: 1, endDay: 0, scheduledDays: 0 };

  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  if (joining > today) return { startDay: 1, endDay: 0, scheduledDays: 0 };

  const year = now.getFullYear();
  const monthIndex = now.getMonth();
  const monthDays = getDaysInMonth(year, monthIndex + 1);
  const monthStart = new Date(year, monthIndex, 1);
  const monthEnd = new Date(year, monthIndex, Math.min(monthDays, today.getDate()));
  const start = joining > monthStart ? joining : monthStart;
  if (start > monthEnd) return { startDay: 1, endDay: 0, scheduledDays: 0 };

  return {
    startDay: start.getDate(),
    endDay: monthEnd.getDate(),
    scheduledDays: getWorkingDaysInRange(start, monthEnd),
  };
}

export function getAttendanceData(employee, attendance = {}, now = new Date()) {
  const month = now.getMonth() + 1;
  const year = now.getFullYear();
  const monthDays = getDaysInMonth(year, month);
  const todayDay = now.getDate();

  if (!employee) {
    return { present: 0, absent: 0, unmarked: 0, scheduled: 0, beforeJoining: 0, future: Math.max(0, monthDays - todayDay) };
  }

  const { startDay, endDay, scheduledDays } = getEmployeeEligibleRange(employee, now);
  let present = 0;
  let absent = 0;
  let unmarked = 0;

  for (let day = Math.max(1, startDay); day <= Math.min(monthDays, endDay); day += 1) {
    const date = new Date(year, month - 1, day);
    // Weekend and public-holiday dates are excluded from both attendance counts and payroll days.
    if (!isWorkingDay(date)) continue;
    const status = attendance[`${employee.id}_${day}`];
    if (status === "present") present += 1;
    else if (status === "absent") absent += 1;
    else unmarked += 1;
  }

  return {
    present,
    absent,
    unmarked,
    scheduled: scheduledDays,
    beforeJoining: Math.max(0, startDay - 1),
    future: Math.max(0, monthDays - todayDay),
  };
}

export function calculateEMI(principal, annualRate, months) {
  const amount = Number(principal);
  const rateInput = Number(annualRate);
  const term = Number(months);
  if (!Number.isFinite(amount) || amount <= 0 || !Number.isFinite(term) || term <= 0) return 0;
  if (!Number.isFinite(rateInput) || rateInput < 0) return 0;
  if (!rateInput) return amount / term;
  const rate = rateInput / 12 / 100;
  return (amount * rate * Math.pow(1 + rate, term)) / (Math.pow(1 + rate, term) - 1);
}

export function getSalaryData(employee, attendance = {}, loans = [], now = new Date()) {
  if (!employee) {
    return { present: 0, absent: 0, unmarked: 0, scheduled: 0, baseToDate: 0, deduction: 0, adjusted: 0, emi: 0, net: 0 };
  }

  const attendanceData = getAttendanceData(employee, attendance, now);
  const totalWorkingDays = getWorkingDaysInMonth(now.getFullYear(), now.getMonth() + 1);
  const monthlySalary = Number(employee.salary) || 0;
  const dailyRate = totalWorkingDays > 0 ? monthlySalary / totalWorkingDays : 0;
  const baseToDate = totalWorkingDays > 0 ? monthlySalary * (attendanceData.scheduled / totalWorkingDays) : 0;
  const allowedAbsentDays = Math.min(3, attendanceData.scheduled);
  const extraAbsentDays = Math.max(0, attendanceData.absent - allowedAbsentDays);
  const deduction = extraAbsentDays * dailyRate;
  const adjusted = Math.max(0, baseToDate - deduction);

  const emi = loans
    .filter((loan) => String(loan.employeeId) === String(employee.id) && Number(loan.outstanding) > 0)
    .reduce((total, loan) => total + calculateEMI(loan.amount, loan.interest, loan.tenure), 0);

  return {
    present: attendanceData.present,
    absent: attendanceData.absent,
    unmarked: attendanceData.unmarked,
    scheduled: attendanceData.scheduled,
    baseToDate,
    deduction,
    adjusted,
    emi,
    net: Math.max(0, adjusted - emi),
  };
}

export function validateLoanInput({ amount, interest, tenure, maxLoanAmount = Infinity }) {
  const loanAmount = Number(amount);
  const annualInterest = Number(interest);
  const loanTenure = Number(tenure);
  const loanCap = Number(maxLoanAmount);

  if (!Number.isFinite(loanAmount) || loanAmount <= 0) {
    return "Loan amount must be greater than zero.";
  }
  if (Number.isFinite(loanCap) && loanCap >= 0 && loanAmount > loanCap + 0.000001) {
    return `Loan amount cannot exceed 50% of the employee's monthly salary (₹${loanCap.toLocaleString("en-IN", { maximumFractionDigits: 2 })}).`;
  }
  if (!Number.isFinite(annualInterest) || annualInterest < 0 || annualInterest > 100) {
    return "Interest must be between 0 and 100%.";
  }
  if (!Number.isInteger(loanTenure) || loanTenure < 1 || loanTenure > 120) {
    return "Tenure must be between 1 and 120 months.";
  }
  return "";
}

export function calculateTotalMonthlyEmi(loans, employeeId) {
  return (loans || [])
    .filter((loan) => String(loan.employeeId) === String(employeeId) && Number(loan.outstanding) > 0)
    .reduce((total, loan) => total + calculateEMI(loan.amount, loan.interest, loan.tenure), 0);
}
