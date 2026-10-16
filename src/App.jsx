import { useEffect, useMemo, useState } from "react";
import "./App.css";
import Auth from "./Auth.jsx";
import { getTodayInputValue, parseLocalDate, getDaysInMonth, calculateEMI, calculateTotalMonthlyEmi, getAttendanceData, getSalaryData, validateEmployeeInput, validateLoanInput, getHolidayName, isWorkingDay } from "./emsLogic.js";

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [authUser, setAuthUser] = useState(null);
  const [authChecking, setAuthChecking] = useState(true);
  const [employees, setEmployees] = useState([]);
  const [attendance, setAttendance] = useState({});
  const [loans, setLoans] = useState([]);
  const [profile, setProfile] = useState({
    companyName: "EMS",
    email: "admin@ems.com",
    phone: "+91 98765 43210",
    systemName: "Employee Management System",
  });

  const [currentPage, setCurrentPage] = useState("dashboard");
  const [employeeModal, setEmployeeModal] = useState(false);
  const [loanModal, setLoanModal] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState(null);
  const [search, setSearch] = useState("");
  const [departmentFilter, setDepartmentFilter] = useState("");
  const [attendanceEmployee, setAttendanceEmployee] = useState("");
  const [salaryEmployee, setSalaryEmployee] = useState("");
  const [toastMessage, setToastMessage] = useState("");

  const [employeeForm, setEmployeeForm] = useState({
    name: "",
    age: "",
    email: "",
    phone: "",
    dept: "Management",
    designation: "",
    salary: "",
    joining: "",
    leaveBalance: 10,
    status: "Active",
  });

  const [loanForm, setLoanForm] = useState({
    employeeId: "",
    amount: "",
    interest: 8,
    tenure: 12,
  });

  const now = new Date();
  const currentMonth = now.getMonth() + 1;
  const currentYear = now.getFullYear();
  const todayString = getTodayInputValue(now);
  const monthDays = getDaysInMonth(currentYear, currentMonth);
  const todayDay = now.getDate();

  function getApiHeaders() {
    const token = localStorage.getItem("ems_jwt");
    return {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
  }

  async function apiRequest(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: { ...getApiHeaders(), ...(options.headers || {}) },
    });

    const text = await response.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = text;
    }

    if (response.status === 401 && !path.startsWith("/api/auth/")) {
      localStorage.removeItem("ems_jwt");
      localStorage.removeItem("ems_user");
      setAuthUser(null);
      setIsAuthenticated(false);
    }

    if (!response.ok) {
      throw new Error(
        data?.message || data || `Request failed with status ${response.status}`
      );
    }

    return data;
  }

  function formatApiEmployee(employee) {
    return {
      id: String(employee.employeeId),
      name: employee.employeeName || "",
      age: Number(employee.age || 0),
      email: employee.email || "",
      phone: employee.phone || "",
      dept: employee.department || "",
      designation: employee.designation || "",
      salary: Number(employee.baseSalary || 0),
      joining: employee.joiningDate || "",
      status: employee.status || "Active",
      daysPresent: Number(employee.daysPresent ?? 0),
      leaveBalance: Number(employee.leaveBalance ?? 10),
      loanAmount: Number(employee.loanAmount ?? 0),
      dailyAttendanceStatus: employee.dailyAttendanceStatus || "NOT MARKED",
    };
  }

  async function loadEmployeesFromApi() {
    const data = await apiRequest("/api/employees");
    setEmployees((data || []).map(formatApiEmployee));
  }

  async function loadAttendanceFromApi() {
    const data = await apiRequest(`/api/attendance?month=${currentMonth}&year=${currentYear}`);
    const mapped = {};
    (data || []).forEach((item) => {
      mapped[attendanceKey(item.employeeId, item.day)] = item.status;
    });
    setAttendance(mapped);
  }

  async function loadLoansFromApi() {
    const data = await apiRequest("/api/loans");
    setLoans(data || []);
  }

  async function loadProfileFromApi() {
    const data = await apiRequest("/api/company-profile");
    if (data) setProfile(data);
  }

  async function loadAllData() {
    try {
      const results = await Promise.allSettled([
        loadEmployeesFromApi(),
        loadAttendanceFromApi(),
        loadLoansFromApi(),
        loadProfileFromApi(),
      ]);
      const failed = results.find((result) => result.status === "rejected");
      if (failed) throw failed.reason;
    } catch (error) {
      console.error("API data loading error:", error);
      showToast(error.message || "Could not load data from the server");
    }
  }

  useEffect(() => {
    const token = localStorage.getItem("ems_jwt");
    if (!token) {
      setAuthChecking(false);
      return;
    }

    fetch(`${API_BASE}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(async (response) => {
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Invalid session");
        setAuthUser(data);
        setIsAuthenticated(true);
      })
      .catch(() => {
        localStorage.removeItem("ems_jwt");
        localStorage.removeItem("ems_user");
        setIsAuthenticated(false);
      })
      .finally(() => setAuthChecking(false));
  }, []);

  useEffect(() => {
    if (isAuthenticated) loadAllData();
  }, [isAuthenticated]);

  useEffect(() => {
    if (!employees.length) {
      setAttendanceEmployee("");
      setSalaryEmployee("");
      setLoanForm((previous) => ({ ...previous, employeeId: "" }));
      return;
    }

    const firstId = String(employees[0].id);
    if (!employees.some((employee) => String(employee.id) === String(attendanceEmployee))) {
      setAttendanceEmployee(firstId);
    }
    if (!employees.some((employee) => String(employee.id) === String(salaryEmployee))) {
      setSalaryEmployee(firstId);
    }
    if (!loanForm.employeeId || !employees.some((employee) => String(employee.id) === String(loanForm.employeeId))) {
      setLoanForm((previous) => ({ ...previous, employeeId: firstId }));
    }
  }, [employees]);

  function handleLogin(user) {
    setAuthUser(user);
    setIsAuthenticated(true);
  }

  function handleLogout() {
    localStorage.removeItem("ems_jwt");
    localStorage.removeItem("ems_user");
    setAuthUser(null);
    setIsAuthenticated(false);
    setEmployees([]);
    setAttendance({});
    setLoans([]);
    setCurrentPage("dashboard");
  }

  function money(value) {
    return Number(value || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 });
  }

  function formatDate(date) {
    const parsed = parseLocalDate(date);
    if (!parsed) return "-";
    return parsed.toLocaleDateString("en-IN", {
      day: "2-digit", month: "short", year: "numeric",
    });
  }

  function initials(name) {
    return (name || "EMS").split(" ").map((word) => word[0]).slice(0, 2).join("").toUpperCase();
  }

  function showToast(message) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(""), 2500);
  }

  function attendanceKey(employeeId, day) {
    return `${employeeId}_${day}`;
  }

  function changePage(page) { setCurrentPage(page); }

  function openEmployeeModal(employee = null) {
    setEditingEmployee(employee);
    setEmployeeForm(employee ? {
      name: employee.name,
      age: employee.age,
      email: employee.email,
      phone: employee.phone,
      dept: employee.dept,
      designation: employee.designation,
      salary: employee.salary,
      joining: employee.joining,
      leaveBalance: employee.leaveBalance,
      status: employee.status,
    } : {
      name: "", age: "", email: "", phone: "", dept: "Management", designation: "",
      salary: "", joining: "", leaveBalance: 10, status: "Active",
    });
    setEmployeeModal(true);
  }

  async function saveEmployee(event) {
    event.preventDefault();
    const validationError = validateEmployeeInput(employeeForm, todayString);
    if (validationError) {
      showToast(validationError);
      return;
    }
    if (editingEmployee && Number(employeeForm.salary) !== Number(editingEmployee.salary)) {
      const existingEmi = calculateTotalMonthlyEmi(loans, editingEmployee.id);
      const maximumEmi = Number(employeeForm.salary) * 0.5;
      if (existingEmi > maximumEmi + 0.000001) {
        showToast(`Salary cannot be reduced below the level needed for existing loan EMIs (50% cap = ₹${money(maximumEmi)}).`);
        return;
      }
    }

    try {
      const employeeId = editingEmployee ? editingEmployee.id : `E${Date.now()}`;
      const employeeData = {
        employeeId: String(employeeId),
        employeeName: employeeForm.name.trim(),
        age: Number(employeeForm.age),
        department: employeeForm.dept,
        designation: employeeForm.designation.trim(),
        baseSalary: Number(employeeForm.salary),
        email: employeeForm.email.trim(),
        phone: employeeForm.phone.trim(),
        joiningDate: employeeForm.joining,
        leaveBalance: Number(employeeForm.leaveBalance),
        status: employeeForm.status,
      };

      const saved = await apiRequest(
        editingEmployee ? `/api/employees/${editingEmployee.id}` : "/api/employees",
        { method: editingEmployee ? "PUT" : "POST", body: JSON.stringify(employeeData) }
      );
      const formatted = formatApiEmployee(saved);
      if (editingEmployee) setEmployees((prev) => prev.map((e) => e.id === editingEmployee.id ? formatted : e));
      else setEmployees((prev) => [...prev, formatted]);
      setEmployeeModal(false);
      setEditingEmployee(null);
      showToast(editingEmployee ? "Employee updated successfully" : "Employee added successfully");
    } catch (error) {
      console.error(error);
      showToast(error.message || "Failed to save employee");
    }
  }

  async function deleteEmployee(id) {
    if (!window.confirm("Delete this employee and all of their attendance and loan records?")) return;
    try {
      await apiRequest(`/api/employees/${id}`, { method: "DELETE" });
      setEmployees((prev) => prev.filter((e) => String(e.id) !== String(id)));
      setLoans((prev) => prev.filter((loan) => String(loan.employeeId) !== String(id)));
      setAttendance((prev) => {
        const next = { ...prev };
        Object.keys(next).forEach((key) => { if (key.startsWith(`${id}_`)) delete next[key]; });
        return next;
      });
      showToast("Employee deleted successfully");
    } catch (error) { showToast(error.message || "Failed to delete employee"); }
  }

  async function toggleAttendance(day) {
    if (!attendanceEmployee) return;
    const employee = employees.find((item) => String(item.id) === String(attendanceEmployee));
    if (!employee) return;
    if (employee.status !== "Active") return showToast("Attendance cannot be marked for an inactive employee.");
    const joining = parseLocalDate(employee.joining);
    const dayDate = new Date(currentYear, currentMonth - 1, day);
    const current = attendance[attendanceKey(attendanceEmployee, day)] || "";
    if (joining && dayDate < joining) return showToast("Attendance cannot be marked before the joining date.");
    if (dayDate > new Date(now.getFullYear(), now.getMonth(), now.getDate())) return showToast("Future attendance cannot be marked.");
    const holidayName = getHolidayName(dayDate);
    if (holidayName) return showToast(`Attendance cannot be marked on a weekend or public holiday: ${holidayName}.`);
    if (!isWorkingDay(dayDate)) return showToast("Attendance can only be marked on working days.");

    const next = current === "" ? "present" : current === "present" ? "absent" : "";
    try {
      const base = { employeeId: String(attendanceEmployee), day, month: currentMonth, year: currentYear };
      if (next === "") {
        await apiRequest(`/api/attendance?employeeId=${encodeURIComponent(base.employeeId)}&day=${day}&month=${currentMonth}&year=${currentYear}`, { method: "DELETE" });
        setAttendance((prev) => { const copy = { ...prev }; delete copy[attendanceKey(attendanceEmployee, day)]; return copy; });
        showToast(`Day ${day} cleared`);
      } else {
        await apiRequest("/api/attendance", { method: "POST", body: JSON.stringify({ ...base, status: next }) });
        setAttendance((prev) => ({ ...prev, [attendanceKey(attendanceEmployee, day)]: next }));
        showToast(`Day ${day} marked ${next}`);
      }
    } catch (error) { showToast(error.message || "Failed to save attendance"); }
  }

  function openLoanModal() {
    const firstActiveEmployee = employees.find((employee) => employee.status === "Active");
    if (!firstActiveEmployee) {
      showToast("Add at least one active employee before creating a loan.");
      return;
    }
    setLoanForm({ employeeId: firstActiveEmployee.id, amount: "", interest: 8, tenure: 12 });
    setLoanModal(true);
  }

  async function createLoan(event) {
    event.preventDefault();
    const amount = Number(loanForm.amount);
    const interest = Number(loanForm.interest);
    const tenure = Number(loanForm.tenure);
    const employee = employees.find((item) => String(item.id) === String(loanForm.employeeId));
    if (!employee) return showToast("Select a valid employee.");
    if (employee.status !== "Active") return showToast("A new loan can only be created for an active employee.");
    const joining = parseLocalDate(employee.joining);
    if (joining && joining > now) return showToast("Employee has not joined yet.");
    const maxPrincipal = Number(employee.salary) * 0.5;
    const validationError = validateLoanInput({ amount, interest, tenure, maxLoanAmount: maxPrincipal });
    if (validationError) return showToast(validationError);

    const emi = calculateEMI(amount, interest, tenure);
    const existingEmi = calculateTotalMonthlyEmi(loans, employee.id);
    const maxTotalEmi = Number(employee.salary) * 0.5;
    if (existingEmi + emi > maxTotalEmi + 0.000001) {
      return showToast(`Total monthly loan EMI cannot exceed 50% of salary (₹${money(maxTotalEmi)}).`);
    }

    try {
      const loan = await apiRequest("/api/loans", {
        method: "POST",
        body: JSON.stringify({ employeeId: String(loanForm.employeeId), amount, interest, tenure, outstanding: amount }),
      });
      setLoans((prev) => [loan, ...prev]);
      setLoanModal(false);
      showToast("Loan created successfully");
    } catch (error) { showToast(error.message || "Failed to create loan"); }
  }

  async function deleteLoan(id) {
    if (!window.confirm("Delete this loan record?")) return;
    try {
      await apiRequest(`/api/loans/${id}`, { method: "DELETE" });
      setLoans((prev) => prev.filter((loan) => loan.id !== id));
      showToast("Loan deleted successfully");
    } catch (error) { showToast(error.message || "Failed to delete loan"); }
  }

  async function payLoan(loan) {
    const outstanding = Number(loan.outstanding || 0);
    if (outstanding <= 0) {
      showToast("This loan is already fully paid.");
      return;
    }

    const suggested = Math.min(
      outstanding,
      calculateEMI(loan.amount, loan.interest, loan.tenure)
    );
    const raw = window.prompt(
      `Enter payment amount (maximum ₹${money(outstanding)}).`,
      suggested.toFixed(2)
    );
    if (raw === null) return;

    const amount = Number(raw);
    if (!Number.isFinite(amount) || amount <= 0) {
      showToast("Payment amount must be greater than zero.");
      return;
    }
    if (amount > outstanding + 0.000001) {
      showToast("Payment cannot exceed the outstanding loan amount.");
      return;
    }

    try {
      const updated = await apiRequest(
        `/api/loans/${loan.id}/payment?amount=${encodeURIComponent(amount)}`,
        { method: "POST" }
      );
      setLoans((prev) => prev.map((item) => item.id === loan.id ? updated : item));
      await loadEmployeesFromApi();
      showToast(updated.outstanding > 0 ? "Loan payment recorded" : "Loan fully paid");
    } catch (error) {
      showToast(error.message || "Failed to record loan payment");
    }
  }

  async function saveProfile() {
    const companyName = String(profile.companyName || "").trim();
    const systemName = String(profile.systemName || "").trim();
    const email = String(profile.email || "").trim();
    const phone = String(profile.phone || "").trim();
    if (companyName.length < 2 || companyName.length > 150 || !/^[A-Za-z0-9][A-Za-z0-9 &().,'-]{1,149}$/.test(companyName)) return showToast("Company name must be 2-150 valid characters.");
    if (systemName.length < 2 || systemName.length > 150 || !/^[A-Za-z0-9][A-Za-z0-9 &().,'-]{1,149}$/.test(systemName)) return showToast("System name must be 2-150 valid characters.");
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return showToast("Enter a valid company email address.");
    if (phone && !/^(?=(?:\D*\d){7,15}\D*$)[0-9+()\-\s]{7,20}$/.test(phone)) return showToast("Enter a valid company phone number.");
    try {
      const saved = await apiRequest("/api/company-profile", {
        method: "PUT",
        body: JSON.stringify({ ...profile, companyName, systemName, email, phone }),
      });
      setProfile(saved);
      showToast("Company information saved");
    } catch (error) { showToast(error.message || "Failed to save company information"); }
  }

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[char]));
  }

  function printSalary(id) {
    const employee = employees.find((item) => String(item.id) === String(id));
    if (!employee) return;
    const salaryData = getSalaryData(employee, attendance, loans, now);
    const printWindow = window.open("", "_blank");
    if (!printWindow) { showToast("Please allow pop-ups to print the salary slip"); return; }
    printWindow.document.write(`
      <html><head><title>EMS Salary Slip</title><style>
      body{font-family:Arial;padding:40px;color:#222} h1{color:#9b762d} table{width:100%;border-collapse:collapse;margin-top:30px} td{padding:13px;border-bottom:1px solid #ddd}.total{font-size:20px;font-weight:bold}
      </style></head><body><h1>${escapeHtml(profile.companyName || "EMS")}</h1><h2>Employee Salary Slip</h2>
      <p><b>Employee:</b> ${escapeHtml(employee.name)}</p><p><b>Department:</b> ${escapeHtml(employee.dept)}</p><p><b>Designation:</b> ${escapeHtml(employee.designation)}</p>
      <table><tr><td>Monthly Salary</td><td>₹${money(employee.salary)}</td></tr><tr><td>Scheduled Days</td><td>${salaryData.scheduled}</td></tr><tr><td>Present Days</td><td>${salaryData.present}</td></tr><tr><td>Absent Days</td><td>${salaryData.absent}</td></tr><tr><td>Attendance Deduction</td><td>- ₹${money(salaryData.deduction)}</td></tr><tr><td>Loan EMI</td><td>- ₹${money(salaryData.emi)}</td></tr><tr class="total"><td>NET PAYABLE (TO DATE)</td><td>₹${money(salaryData.net)}</td></tr></table></body></html>`);
    printWindow.document.close(); printWindow.print();
  }

  const filteredEmployees = useMemo(() => {
    return employees.filter((employee) => {
      const matchesSearch =
        employee.name
          .toLowerCase()
          .includes(search.toLowerCase()) ||
        employee.email
          .toLowerCase()
          .includes(search.toLowerCase()) ||
        employee.designation
          .toLowerCase()
          .includes(search.toLowerCase());

      const matchesDepartment =
        !departmentFilter ||
        employee.dept === departmentFilter;

      return matchesSearch && matchesDepartment;
    });
  }, [employees, search, departmentFilter]);

  const latestEmployees = useMemo(() => {
    return [...employees]
      .sort(
        (a, b) =>
          new Date(b.joining) - new Date(a.joining)
      )
      .slice(0, 5);
  }, [employees]);

  const dashboardData = useMemo(() => {
    let payroll = 0;
    let present = 0;
    let absent = 0;

    employees.forEach((employee) => {
      const salaryData = getSalaryData(employee, attendance, loans, now);

      payroll += salaryData.net;
      present += salaryData.present;
      absent += salaryData.absent;
    });

    const total = present + absent;

    const percentage = total
      ? Math.round((present / total) * 100)
      : 0;

    const outstanding = loans.reduce(
      (totalAmount, loan) =>
        totalAmount + Number(loan.outstanding),
      0
    );

    const today = new Date().getDate();

    const presentToday = employees.filter((employee) => {
      const joining = parseLocalDate(employee.joining);
      const eligibleToday = !joining || joining <= now;
      return eligibleToday && attendance[attendanceKey(employee.id, today)] === "present";
    }).length;

    return {
      payroll,
      present,
      absent,
      percentage,
      outstanding,
      presentToday,
    };
  }, [employees, attendance, loans]);

  const selectedAttendanceData = getAttendanceData(
    employees.find((employee) => String(employee.id) === String(attendanceEmployee)),
    attendance,
    now
  );

  const selectedAttendancePercent =
    selectedAttendanceData.present +
      selectedAttendanceData.absent >
    0
      ? Math.round(
          (selectedAttendanceData.present /
            (selectedAttendanceData.present +
              selectedAttendanceData.absent)) *
            100
        )
      : 0;

  const selectedSalaryEmployee = employees.find(
    (employee) => String(employee.id) === String(salaryEmployee)
  );

  const selectedSalaryData = getSalaryData(
    selectedSalaryEmployee,
    attendance,
    loans,
    now
  );

  const loanPreview = calculateEMI(
    Number(loanForm.amount),
    Number(loanForm.interest),
    Number(loanForm.tenure)
  );
  const selectedLoanEmployee = employees.find(
    (employee) => String(employee.id) === String(loanForm.employeeId)
  );
  const existingLoanEmi = selectedLoanEmployee
    ? calculateTotalMonthlyEmi(loans, selectedLoanEmployee.id)
    : 0;
  const totalLoanEmiAfterNew = existingLoanEmi + loanPreview;
  const maxLoanEmi = selectedLoanEmployee
    ? Number(selectedLoanEmployee.salary) * 0.5
    : 0;

  const pageTitles = {
    dashboard: "Dashboard",
    employees: "Employees",
    attendance: "Attendance",
    salary: "Salary & Payroll",
    loans: "Employee Loans",
    profile: "Company Profile",
  };

  if (authChecking) {
    return (
      <div className="auth-shell">
        <div className="auth-card">
          <div className="auth-brand">EMS</div>
          <p className="auth-description" style={{ textAlign: "center", marginTop: "20px" }}>
            Checking your authentication...
          </p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Auth onLogin={handleLogin} />;
  }

  return (
    <>
      <aside className="sidebar">
        <div className="logo">
          EMS

          <span className="logo-sub">
            EMPLOYEE MANAGEMENT SYSTEM
          </span>
        </div>

        <div className="nav-title">
          Management
        </div>

        <button
          className={`nav-btn ${
            currentPage === "dashboard" ? "active" : ""
          }`}
          onClick={() => changePage("dashboard")}
        >
          ◈ &nbsp; Dashboard
        </button>

        <button
          className={`nav-btn ${
            currentPage === "employees" ? "active" : ""
          }`}
          onClick={() => changePage("employees")}
        >
          ♙ &nbsp; Employees
        </button>

        <button
          className={`nav-btn ${
            currentPage === "attendance" ? "active" : ""
          }`}
          onClick={() => changePage("attendance")}
        >
          ◷ &nbsp; Attendance
        </button>

        <button
          className={`nav-btn ${
            currentPage === "salary" ? "active" : ""
          }`}
          onClick={() => changePage("salary")}
        >
          ₹ &nbsp; Salary & Payroll
        </button>

        <button
          className={`nav-btn ${
            currentPage === "loans" ? "active" : ""
          }`}
          onClick={() => changePage("loans")}
        >
          ◇ &nbsp; Employee Loans
        </button>

        <div className="nav-title">
          System
        </div>

        <button
          className={`nav-btn ${
            currentPage === "profile" ? "active" : ""
          }`}
          onClick={() => changePage("profile")}
        >
          ◎ &nbsp; Company Profile
        </button>
      </aside>

      <main className="main">
        <header className="topbar">
          <div>
            <h1>
              {pageTitles[currentPage]}
            </h1>

            <div className="subtitle">
              Employee Management System
            </div>
          </div>

          <div className="admin">
            <div>
              <strong>
                {authUser?.name || "EMS Administrator"}
              </strong>

              <small
                style={{
                  display: "block",
                  color: "var(--muted)",
                  fontSize: "11px",
                  marginTop: "3px",
                }}
              >
                EMPLOYER · {authUser?.email || ""}
              </small>
            </div>

            <div className="admin-avatar">
              {initials(authUser?.name || "EMS")}
            </div>

            <button
              className="secondary"
              onClick={handleLogout}
              style={{ padding: "9px 12px" }}
            >
              Logout
            </button>
          </div>
        </header>

        {currentPage === "dashboard" && (
          <section className="page active">
            <div className="stats">
              <div className="card stat">
                <div className="stat-icon">
                  ♙
                </div>

                <div className="stat-label">
                  Total Employees
                </div>

                <div className="stat-value">
                  {employees.length}
                </div>
              </div>

              <div className="card stat">
                <div className="stat-icon">
                  ✓
                </div>

                <div className="stat-label">
                  Present Today
                </div>

                <div className="stat-value">
                  {dashboardData.presentToday}
                </div>
              </div>

              <div className="card stat">
                <div className="stat-icon">
                  ₹
                </div>

                <div className="stat-label">
                  Payroll to Date
                </div>

                <div className="stat-value">
                  ₹{money(dashboardData.payroll)}
                </div>
              </div>

              <div className="card stat">
                <div className="stat-icon">
                  ◇
                </div>

                <div className="stat-label">
                  Outstanding Loans
                </div>

                <div className="stat-value">
                  ₹{money(dashboardData.outstanding)}
                </div>
              </div>
            </div>

            <div className="dashboard-grid">
              <div className="card">
                <div className="section-head">
                  <div>
                    <h2>
                      Latest Employees
                    </h2>

                    <p
                      style={{
                        color: "var(--muted)",
                        fontSize: "11px",
                        marginTop: "5px",
                      }}
                    >
                      Recently joined employees
                    </p>
                  </div>

                  <button
                    className="view-btn"
                    onClick={() =>
                      changePage("employees")
                    }
                  >
                    View All
                  </button>
                </div>

                <div className="latest-list">
                  {latestEmployees.map(
                    (employee) => (
                      <div
                        className="latest-item"
                        key={employee.id}
                      >
                        <div className="emp-avatar">
                          {initials(employee.name)}
                        </div>

                        <div>
                          <div className="emp-name">
                            {employee.name}
                          </div>

                          <div className="emp-info">
                            {employee.designation}
                            {" · "}
                            {employee.dept}
                          </div>
                        </div>

                        <div className="emp-salary">
                          ₹{money(employee.salary)}

                          <div className="emp-date">
                            Joined{" "}
                            {formatDate(
                              employee.joining
                            )}
                          </div>
                        </div>
                      </div>
                    )
                  )}
                </div>
              </div>

              <div className="card">
                <div className="section-head">
                  <h2>
                    Attendance Overview
                  </h2>

                  <button
                    className="view-btn"
                    onClick={() =>
                      changePage("attendance")
                    }
                  >
                    Manage
                  </button>
                </div>

                <div className="attendance-big">
                  {dashboardData.percentage}%
                </div>

                <p
                  style={{
                    color: "var(--muted)",
                    fontSize: "11px",
                  }}
                >
                  Overall attendance this month
                </p>

                <div className="progress">
                  <span
                    style={{
                      width: `${dashboardData.percentage}%`,
                    }}
                  />
                </div>

                <div className="att-row">
                  <span>
                    Present Days
                  </span>

                  <strong>
                    {dashboardData.present}
                  </strong>
                </div>

                <div className="att-row">
                  <span>
                    Absent Days
                  </span>

                  <strong>
                    {dashboardData.absent}
                  </strong>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="section-head">
                <div>
                  <h2>
                    Latest Employee Details
                  </h2>

                  <p
                    style={{
                      color: "var(--muted)",
                      fontSize: "11px",
                      marginTop: "5px",
                    }}
                  >
                    Employee information and current status
                  </p>
                </div>

                <button
                  className="primary"
                  onClick={() =>
                    openEmployeeModal()
                  }
                >
                  + Add Employee
                </button>
              </div>

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th>Department</th>
                      <th>Designation</th>
                      <th>Phone</th>
                      <th>Salary</th>
                      <th>Joining Date</th>
                      <th>Status</th>
                    </tr>
                  </thead>

                  <tbody>
                    {latestEmployees.map(
                      (employee) => (
                        <tr key={employee.id}>
                          <td>
                            <strong>
                              {employee.name}
                            </strong>

                            <div
                              style={{
                                color:
                                  "var(--muted)",
                                fontSize: "10px",
                                marginTop: "3px",
                              }}
                            >
                              {employee.email}
                            </div>
                          </td>

                          <td>
                            {employee.dept}
                          </td>

                          <td>
                            {employee.designation}
                          </td>

                          <td>
                            {employee.phone}
                          </td>

                          <td>
                            ₹
                            {money(
                              employee.salary
                            )}
                          </td>

                          <td>
                            {formatDate(
                              employee.joining
                            )}
                          </td>

                          <td>
                            <span
                              className={`badge ${
                                employee.status ===
                                "Active"
                                  ? "active"
                                  : "inactive"
                              }`}
                            >
                              {employee.status}
                            </span>
                          </td>
                        </tr>
                      )
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {currentPage === "employees" && (
          <section className="page active">
            <div className="card">
              <div className="section-head">
                <h2>
                  Employee Directory
                </h2>

                <button
                  className="primary"
                  onClick={() =>
                    openEmployeeModal()
                  }
                >
                  + Add Employee
                </button>
              </div>

              <div className="search-row">
                <input
                  placeholder="Search employee..."
                  value={search}
                  onChange={(event) =>
                    setSearch(event.target.value)
                  }
                />

                <select
                  value={departmentFilter}
                  onChange={(event) =>
                    setDepartmentFilter(
                      event.target.value
                    )
                  }
                >
                  <option value="">
                    All Departments
                  </option>

                  <option>
                    Management
                  </option>

                  <option>
                    Technology
                  </option>

                  <option>
                    Finance
                  </option>

                  <option>
                    Human Resources
                  </option>

                  <option>
                    Sales
                  </option>

                  <option>
                    Operations
                  </option>
                </select>
              </div>

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th>Department</th>
                      <th>Designation</th>
                      <th>Salary</th>
                      <th>Joining Date</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {filteredEmployees.map(
                      (employee) => (
                        <tr key={employee.id}>
                          <td>
                            <strong>
                              {employee.name}
                            </strong>

                            <div
                              style={{
                                color:
                                  "var(--muted)",
                                fontSize: "10px",
                                marginTop: "3px",
                              }}
                            >
                              {employee.email}
                            </div>
                          </td>

                          <td>
                            {employee.dept}
                          </td>

                          <td>
                            {employee.designation}
                          </td>

                          <td>
                            ₹
                            {money(
                              employee.salary
                            )}
                          </td>

                          <td>
                            {formatDate(
                              employee.joining
                            )}
                          </td>

                          <td>
                            <span
                              className={`badge ${
                                employee.status ===
                                "Active"
                                  ? "active"
                                  : "inactive"
                              }`}
                            >
                              {employee.status}
                            </span>
                          </td>

                          <td className="actions">
                            <button
                              onClick={() =>
                                openEmployeeModal(
                                  employee
                                )
                              }
                            >
                              Edit
                            </button>

                            <button
                              onClick={() =>
                                deleteEmployee(
                                  employee.id
                                )
                              }
                            >
                              Delete
                            </button>
                          </td>
                        </tr>
                      )
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {currentPage === "attendance" && (
          <section className="page active">
            <div className="card">
              <div className="section-head">
                <h2>Attendance Management</h2>
                <select value={attendanceEmployee} onChange={(event) => setAttendanceEmployee(event.target.value)} style={{ maxWidth: "300px" }}>
                  {employees.map((employee) => (
                    <option key={employee.id} value={employee.id}>{employee.name} ({employee.id})</option>
                  ))}
                </select>
              </div>

              {attendanceEmployee && (
                <>
                  <div className="stats">
                    <div className="card"><div className="stat-label">Present</div><div className="stat-value">{selectedAttendanceData.present}</div></div>
                    <div className="card"><div className="stat-label">Absent</div><div className="stat-value">{selectedAttendanceData.absent}</div></div>
                    <div className="card"><div className="stat-label">Not Marked</div><div className="stat-value">{selectedAttendanceData.unmarked}</div></div>
                    <div className="card"><div className="stat-label">Attendance</div><div className="stat-value">{selectedAttendancePercent}%</div></div>
                  </div>

                  <p style={{ color: "var(--muted)", fontSize: "11px", marginBottom: "15px" }}>
                    Only working dates from the employee's joining date through today can be marked. Saturdays, Sundays, Maharashtra public holidays and future dates are unavailable.
                  </p>

                  <div className="att-grid">
                    {Array.from({ length: monthDays }, (_, index) => {
                      const day = index + 1;
                      const employee = employees.find((item) => String(item.id) === String(attendanceEmployee));
                      const joining = parseLocalDate(employee?.joining);
                      const dayDate = new Date(currentYear, currentMonth - 1, day);
                      const beforeJoining = Boolean(joining && dayDate < joining);
                      const future = day > todayDay;
                      const holidayName = getHolidayName(dayDate);
                      const status = attendance[attendanceKey(attendanceEmployee, day)] || "";
                      const inactive = employee?.status !== "Active";
                      const disabled = beforeJoining || future || inactive || Boolean(holidayName);
                      const label = inactive ? "INACTIVE" : beforeJoining ? "BEFORE JOIN" : holidayName ? holidayName : future ? "FUTURE" : status ? status.toUpperCase() : "NOT MARKED";
                      return (
                        <button
                          key={day}
                          type="button"
                          className={`day ${disabled ? "disabled-day" : status === "absent" ? "absent-day" : status === "present" ? "present-day" : "unmarked-day"}`}
                          onClick={() => !disabled && toggleAttendance(day)}
                          disabled={disabled}
                          title={disabled ? label : `Day ${day}: ${label}`}
                        >
                          <small>{label}</small>
                          {day}
                        </button>
                      );
                    })}
                  </div>
                </>
              )}

              {!employees.length && <p style={{ color: "var(--muted)" }}>Add an employee first to manage attendance.</p>}

              {employees.length > 0 && (
                <div className="card attendance-register-card" style={{ marginTop: "20px" }}>
                  <div className="section-head">
                    <div>
                      <h3>Monthly Attendance Register — All Employees</h3>
                      <p style={{ color: "var(--muted)", fontSize: "11px", marginTop: "5px" }}>
                        {new Date(currentYear, currentMonth - 1, 1).toLocaleDateString("en-IN", { month: "long", year: "numeric" })}. P = Present, A = Absent, H = Weekend/public holiday, F = Future, B = Before joining, — = Not marked.
                      </p>
                    </div>
                  </div>
                  <div className="table-wrap attendance-register-wrap">
                    <table className="attendance-register-table">
                      <thead>
                        <tr>
                          <th>Employee</th>
                          {Array.from({ length: monthDays }, (_, index) => {
                            const day = index + 1;
                            const date = new Date(currentYear, currentMonth - 1, day);
                            const holidayName = getHolidayName(date);
                            const weekday = date.toLocaleDateString("en-IN", { weekday: "short" });
                            return <th key={day} title={holidayName || date.toDateString()}><span>{day}</span><small>{weekday}</small></th>;
                          })}
                        </tr>
                      </thead>
                      <tbody>
                        {employees.map((employee) => {
                          const joining = parseLocalDate(employee.joining);
                          return (
                            <tr key={employee.id}>
                              <td className="attendance-register-employee"><strong>{employee.name}</strong><small>{employee.id}</small></td>
                              {Array.from({ length: monthDays }, (_, index) => {
                                const day = index + 1;
                                const date = new Date(currentYear, currentMonth - 1, day);
                                const dateKey = attendanceKey(employee.id, day);
                                const status = attendance[dateKey] || "";
                                const holidayName = getHolidayName(date);
                                let mark = "—";
                                let detail = "Not marked";
                                let statusClass = "att-mark-unmarked";

                                if (holidayName) {
                                  mark = "H";
                                  detail = `Holiday: ${holidayName}`;
                                  statusClass = "att-mark-holiday";
                                } else if (joining && date < joining) {
                                  mark = "B";
                                  detail = "Before joining date";
                                  statusClass = "att-mark-unmarked";
                                } else if (date > new Date(now.getFullYear(), now.getMonth(), now.getDate())) {
                                  mark = "F";
                                  detail = "Future date; attendance is not open yet";
                                  statusClass = "att-mark-future";
                                } else if (status === "present") {
                                  mark = "P";
                                  detail = "Present";
                                  statusClass = "att-mark-present";
                                } else if (status === "absent") {
                                  mark = "A";
                                  detail = "Absent";
                                  statusClass = "att-mark-absent";
                                }

                                return <td key={day} className="attendance-register-cell" title={`${employee.name} — ${date.toLocaleDateString("en-IN")}: ${detail}`}><span className={`att-mark ${statusClass}`}>{mark}</span></td>;
                              })}
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          </section>
        )}

        {currentPage === "salary" && (
          <section className="page active">
            <div className="card">
              <div className="section-head">
                <div>
                  <h2>Salary &amp; Payroll</h2>
                  <p style={{ color: "var(--muted)", fontSize: "11px", marginTop: "5px" }}>
                    Current-month payroll estimate through today. Saturdays, Sundays, and listed Maharashtra public holidays are not working days. Three absent working days are allowed before salary deduction.
                  </p>
                </div>
                <select value={salaryEmployee} onChange={(event) => setSalaryEmployee(event.target.value)} style={{ maxWidth: "230px" }} disabled={!employees.length}>
                  {employees.map((employee) => <option key={employee.id} value={employee.id}>{employee.name}</option>)}
                </select>
              </div>

              {selectedSalaryEmployee && (
                <div>
                  <div className="payroll-grid">
                    <div className="payroll-box"><label>EMPLOYEE</label><strong>{selectedSalaryEmployee.name}</strong></div>
                    <div className="payroll-box"><label>MONTHLY SALARY</label><strong>₹{money(selectedSalaryEmployee.salary)}</strong></div>
                    <div className="payroll-box"><label>ATTENDANCE</label><strong>{selectedSalaryData.present}/{selectedSalaryData.scheduled}</strong></div>
                    <div className="payroll-box"><label>NET PAYABLE TO DATE</label><strong>₹{money(selectedSalaryData.net)}</strong></div>
                  </div>

                  <div className="table-wrap">
                    <table>
                      <tbody>
                        <tr><td>Monthly Salary</td><td style={{ textAlign: "right" }}>₹{money(selectedSalaryEmployee.salary)}</td></tr>
                        <tr><td>Scheduled Employment Days (to date)</td><td style={{ textAlign: "right" }}>{selectedSalaryData.scheduled}</td></tr>
                        <tr><td>Present Days</td><td style={{ textAlign: "right" }}>{selectedSalaryData.present}</td></tr>
                        <tr><td>Absent Days</td><td style={{ textAlign: "right" }}>{selectedSalaryData.absent}</td></tr>
                        <tr><td>Unmarked Days</td><td style={{ textAlign: "right" }}>{selectedSalaryData.unmarked}</td></tr>
                        <tr><td>Base Pay to Date</td><td style={{ textAlign: "right" }}>₹{money(selectedSalaryData.baseToDate)}</td></tr>
                        <tr><td>Attendance Deduction</td><td style={{ textAlign: "right" }}>- ₹{money(selectedSalaryData.deduction)}</td></tr>
                        <tr><td>Loan EMI Deduction</td><td style={{ textAlign: "right" }}>- ₹{money(selectedSalaryData.emi)}</td></tr>
                        <tr style={{ fontSize: "17px", fontWeight: "bold" }}><td>NET PAYABLE (TO DATE)</td><td style={{ textAlign: "right", color: "var(--gold2)" }}>₹{money(selectedSalaryData.net)}</td></tr>
                      </tbody>
                    </table>
                  </div>

                  <button className="primary" style={{ marginTop: "20px" }} onClick={() => printSalary(selectedSalaryEmployee.id)}>
                    Print Salary Slip
                  </button>
                </div>
              )}
              {!employees.length && <p style={{ color: "var(--muted)" }}>Add an employee first to view payroll.</p>}
            </div>
          </section>
        )}

        {currentPage === "loans" && (
          <section className="page active">
            <div className="card">
              <div className="section-head">
                <h2>
                  Employee Loans
                </h2>

                <button
                  className="primary"
                  onClick={openLoanModal}
                  disabled={!employees.some((employee) => employee.status === "Active")}
                >
                  + New Loan
                </button>
              </div>

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th>Loan Amount</th>
                      <th>Interest</th>
                      <th>Tenure</th>
                      <th>Monthly EMI</th>
                      <th>Outstanding</th>
                      <th>Status</th>
                    
                      <th>Actions</th></tr>
                  </thead>

                  <tbody>
                    {loans.length === 0 && (
                      <tr>
                        <td
                          colSpan="8"
                          style={{
                            textAlign: "center",
                            color:
                              "var(--muted)",
                          }}
                        >
                          No loans recorded.
                        </td>
                      </tr>
                    )}

                    {loans.map((loan) => {
                      const employee =
                        employees.find(
                          (item) =>
                            String(item.id) ===
                            String(loan.employeeId)
                        );

                      const employeeDisplayName = employee?.name || loan.employeeName || loan.employeeId || "Unknown employee";

                      const emi = calculateEMI(
                        loan.amount,
                        loan.interest,
                        loan.tenure
                      );

                      return (
                        <tr key={loan.id}>
                          <td><strong>{employeeDisplayName}</strong><small style={{ display: "block", color: "var(--muted)", fontSize: "10px", marginTop: "3px" }}>{loan.employeeId}</small></td>
                          <td>₹{money(loan.amount)}</td>
                          <td>{loan.interest}%</td>
                          <td>{loan.tenure} months</td>
                          <td>₹{money(emi)}</td>
                          <td>₹{money(loan.outstanding)}</td>
                          <td><span className="badge loan-badge">{loan.outstanding > 0 ? "Active" : "Paid"}</span></td>
                          <td className="actions">{loan.outstanding > 0 && <button onClick={() => payLoan(loan)}>Pay</button>}<button onClick={() => deleteLoan(loan.id)}>Delete</button></td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {currentPage === "profile" && (
          <section className="page active">
            <div className="card profile-card">
              <div className="profile-header">
                <div className="profile-avatar">
                  EMS
                </div>

                <div>
                  <h2>
                    EMS Administration
                  </h2>

                  <p
                    style={{
                      color: "var(--muted)",
                      fontSize: "12px",
                      marginTop: "5px",
                    }}
                  >
                    Employee Management System
                  </p>
                </div>
              </div>

              <div className="form-grid">
                <div className="field">
                  <label>
                    COMPANY NAME
                  </label>

                  <input
                    required
                    minLength="2"
                    maxLength="150"
                    pattern="[A-Za-z0-9][A-Za-z0-9 &().,'-]{1,149}"
                    value={profile.companyName}
                    onChange={(event) => setProfile({ ...profile, companyName: event.target.value })}
                  />
                </div>

                <div className="field">
                  <label>
                    COMPANY EMAIL
                  </label>

                  <input
                    type="email"
                    maxLength="150"
                    value={profile.email}
                    onChange={(event) => setProfile({ ...profile, email: event.target.value })}
                  />
                </div>

                <div className="field">
                  <label>
                    PHONE
                  </label>

                  <input
                    maxLength="50"
                    value={profile.phone}
                    onChange={(event) => setProfile({ ...profile, phone: event.target.value })}
                  />
                </div>

                <div className="field">
                  <label>
                    SYSTEM
                  </label>

                  <input
                    value={profile.systemName || "Employee Management System"}
                    disabled
                  />
                </div>
              </div>

              <button
                className="primary"
                style={{
                  marginTop: "20px",
                }}
                onClick={saveProfile}
              >
                Save Changes
              </button>
            </div>
          </section>
        )}
      </main>

      {employeeModal && (
        <div
          className="modal"
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              setEmployeeModal(false);
            }
          }}
        >
          <div className="modal-box">
            <div className="modal-head">
              <h2>
                {editingEmployee
                  ? "Edit Employee"
                  : "Add Employee"}
              </h2>

              <button
                className="close"
                type="button"
                onClick={() =>
                  setEmployeeModal(false)
                }
              >
                ×
              </button>
            </div>

            <form onSubmit={saveEmployee}>
              <div className="form-grid">
                <div className="field">
                  <label>
                    FULL NAME
                  </label>

                  <input
                    required
                    minLength="2"
                    maxLength="100"
                    pattern="[A-Za-z][A-Za-z .'-]{1,99}"
                    title="Use 2-100 letters, spaces, apostrophes, periods or hyphens."
                    value={employeeForm.name}
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        name: event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    AGE
                  </label>

                  <input
                    type="number"
                    min="18"
                    max="100"
                    step="1"
                    required
                    value={employeeForm.age}
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        age: event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    EMAIL
                  </label>

                  <input
                    type="email"
                    required
                    maxLength="150"
                    pattern="[A-Za-z0-9._%+-]+@[Gg][Mm][Aa][Ii][Ll][.][Cc][Oo][Mm]"
                    title="Use a Gmail address ending exactly with @gmail.com."
                    value={employeeForm.email}
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        email: event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    PHONE
                  </label>

                  <input
                    required
                    inputMode="numeric"
                    maxLength="10"
                    pattern="[6-9][0-9]{9}"
                    title="Enter exactly 10 digits starting with 6, 7, 8 or 9."
                    value={employeeForm.phone}
                    onChange={(event) => {
                      const value = event.target.value;
                      if (/^\d{0,10}$/.test(value)) {
                        setEmployeeForm({
                          ...employeeForm,
                          phone: value,
                        });
                      }
                    }}
                  />
                </div>

                <div className="field">
                  <label>
                    DEPARTMENT
                  </label>

                  <select
                    value={employeeForm.dept}
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        dept: event.target.value,
                      })
                    }
                  >
                    <option>
                      Management
                    </option>

                    <option>
                      Technology
                    </option>

                    <option>
                      Finance
                    </option>

                    <option>
                      Human Resources
                    </option>

                    <option>
                      Sales
                    </option>

                    <option>
                      Operations
                    </option>
                  </select>
                </div>

                <div className="field">
                  <label>
                    DESIGNATION
                  </label>

                  <input
                    required
                    minLength="2"
                    maxLength="100"
                    pattern="[A-Za-z0-9][A-Za-z0-9 &()./'-]{1,99}"
                    title="Use 2-100 letters, numbers, spaces or common punctuation."
                    value={
                      employeeForm.designation
                    }
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        designation:
                          event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    MONTHLY SALARY (₹)
                  </label>

                  <input
                    type="number"
                    min="1"
                    step="0.01"
                    required
                    value={employeeForm.salary}
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        salary: event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    JOINING DATE
                  </label>

                  <input
                    type="date"
                    required
                    value={employeeForm.joining}
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        joining: event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    LEAVE BALANCE (DAYS)
                  </label>

                  <input
                    type="number"
                    min="0"
                    max="365"
                    step="1"
                    required
                    value={employeeForm.leaveBalance}
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        leaveBalance: event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    STATUS
                  </label>

                  <select
                    value={employeeForm.status}
                    onChange={(event) =>
                      setEmployeeForm({
                        ...employeeForm,
                        status: event.target.value,
                      })
                    }
                  >
                    <option>
                      Active
                    </option>

                    <option>
                      Inactive
                    </option>
                  </select>
                </div>
              </div>

              <div className="form-actions">
                <button
                  type="button"
                  className="secondary"
                  onClick={() =>
                    setEmployeeModal(false)
                  }
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary"
                >
                  Save Employee
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loanModal && (
        <div
          className="modal"
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              setLoanModal(false);
            }
          }}
        >
          <div className="modal-box">
            <div className="modal-head">
              <h2>
                Create Employee Loan
              </h2>

              <button
                className="close"
                type="button"
                onClick={() =>
                  setLoanModal(false)
                }
              >
                ×
              </button>
            </div>

            <form onSubmit={createLoan}>
              <div className="form-grid">
                <div className="field">
                  <label>
                    EMPLOYEE
                  </label>

                  <select
                    required
                    value={loanForm.employeeId}
                    onChange={(event) =>
                      setLoanForm({
                        ...loanForm,
                        employeeId:
                          event.target.value,
                      })
                    }
                  >
                    {employees.filter((employee) => employee.status === "Active").map(
                    (employee) => (
                      <option key={employee.id} value={employee.id}>
                        {employee.name}
                      </option>
                    )
                  )}
                  </select>
                </div>

                <div className="field">
                  <label>
                    LOAN AMOUNT (₹)
                  </label>

                  <input
                    type="number"
                    min="0.01"
                    max={selectedLoanEmployee ? Number(selectedLoanEmployee.salary) * 0.5 : undefined}
                    step="0.01"
                    required
                    value={loanForm.amount}
                    onChange={(event) =>
                      setLoanForm({
                        ...loanForm,
                        amount: event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    ANNUAL INTEREST (%)
                  </label>

                  <input
                    type="number"
                    min="0"
                    max="100"
                    step="0.1"
                    required
                    value={loanForm.interest}
                    onChange={(event) =>
                      setLoanForm({
                        ...loanForm,
                        interest:
                          event.target.value,
                      })
                    }
                  />
                </div>

                <div className="field">
                  <label>
                    TENURE (MONTHS)
                  </label>

                  <input
                    type="number"
                    min="1"
                    max="120"
                    step="1"
                    required
                    value={loanForm.tenure}
                    onChange={(event) =>
                      setLoanForm({
                        ...loanForm,
                        tenure: event.target.value,
                      })
                    }
                  />
                </div>
              </div>

              <div
                className="payroll-box"
                style={{
                  marginTop: "15px",
                }}
              >
                <label>
                  ESTIMATED MONTHLY EMI
                </label>

                <strong>
                  ₹{money(loanPreview)}
                </strong>
                {selectedLoanEmployee && (
                  <div style={{ color: "var(--muted)", fontSize: "11px", marginTop: "6px" }}>
                    Maximum loan principal: ₹{money(Number(selectedLoanEmployee.salary) * 0.5)} (50% of monthly salary). Existing EMI: ₹{money(existingLoanEmi)} · Total after loan: ₹{money(totalLoanEmiAfterNew)} / ₹{money(maxLoanEmi)} maximum monthly EMI.
                  </div>
                )}
              </div>

              <div className="form-actions">
                <button
                  type="button"
                  className="secondary"
                  onClick={() =>
                    setLoanModal(false)
                  }
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary"
                >
                  Create Loan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div
        id="toast"
        className={
          toastMessage ? "show" : ""
        }
      >
        {toastMessage}
      </div>
    </>
  );
}

export default App;