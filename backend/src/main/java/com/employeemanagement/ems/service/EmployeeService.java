package com.employeemanagement.ems.service;

import com.employeemanagement.ems.model.Attendance;
import com.employeemanagement.ems.model.Employee;
import com.employeemanagement.ems.repository.AttendanceRepository;
import com.employeemanagement.ems.repository.EmployeeRepository;
import com.employeemanagement.ems.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class EmployeeService {
    private static final double MAX_TOTAL_EMI_TO_SALARY_RATIO = 0.50;
    private static final Pattern ID = Pattern.compile("^E[0-9]{10,16}$");
    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'-]{1,99}$");
    private static final Pattern DESIGNATION = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 &()./'-]{1,99}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Set<String> DEPARTMENTS = Set.of("Management", "Technology", "Finance", "Human Resources", "Sales", "Operations");

    private final EmployeeRepository repo;
    private final AttendanceRepository attendance;
    private final LoanRepository loans;

    public EmployeeService(EmployeeRepository repo, AttendanceRepository attendance, LoanRepository loans) {
        this.repo = repo;
        this.attendance = attendance;
        this.loans = loans;
    }

    public Employee add(Employee e) {
        if (e == null) throw new IllegalArgumentException("Employee data is required.");
        normalize(e);
        validate(e);
        if (repo.existsById(e.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already exists.");
        }
        if (repo.findByEmailIgnoreCase(e.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An employee with this email already exists.");
        }
        e.setDaysPresent(0);
        e.setDailyAttendanceStatus("NOT MARKED");
        e.setLoanAmount(0);
        return repo.save(e);
    }

    public List<Employee> all() { return repo.findAll(); }

    public Employee one(String id) { return repo.findById(id).orElse(null); }

    public Employee update(String id, Employee updated) {
        if (updated == null) throw new IllegalArgumentException("Employee data is required.");
        Employee existing = one(id);
        if (existing == null) return null;

        updated.setEmployeeId(id);
        normalize(updated);
        validate(updated);
        validateJoiningDateChange(existing, updated);
        if (Double.compare(existing.getBaseSalary(), updated.getBaseSalary()) != 0) {
            validateExistingLoanAffordability(updated);
        }

        repo.findByEmailIgnoreCase(updated.getEmail()).ifPresent(other -> {
            if (!other.getEmployeeId().equals(id)) {
                throw new IllegalArgumentException("An employee with this email already exists.");
            }
        });

        existing.setEmployeeName(updated.getEmployeeName());
        existing.setAge(updated.getAge());
        existing.setDepartment(updated.getDepartment());
        existing.setDesignation(updated.getDesignation());
        existing.setBaseSalary(updated.getBaseSalary());
        existing.setLeaveBalance(updated.getLeaveBalance());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setJoiningDate(updated.getJoiningDate());
        existing.setStatus(updated.getStatus());

        return repo.save(existing);
    }

    @Transactional
    public void delete(String id) {
        if (!repo.existsById(id)) throw new IllegalArgumentException("Employee not found.");
        attendance.deleteByEmployeeId(id);
        loans.deleteByEmployeeId(id);
        repo.deleteById(id);
    }


    private void validateExistingLoanAffordability(Employee employee) {
        double totalEmi = loans.findByEmployeeIdOrderByIdDesc(employee.getEmployeeId()).stream()
                .filter(loan -> loan.getOutstanding() > 0)
                .mapToDouble(this::calculateEmi)
                .sum();
        double maximum = employee.getBaseSalary() * MAX_TOTAL_EMI_TO_SALARY_RATIO;
        if (totalEmi > maximum + 0.000001) {
            throw new IllegalArgumentException("Existing monthly loan EMI cannot exceed 50% of the employee's monthly salary.");
        }
    }

    private double calculateEmi(com.employeemanagement.ems.model.Loan loan) {
        double monthlyRate = loan.getInterest() / 12.0 / 100.0;
        if (monthlyRate == 0) return loan.getAmount() / loan.getTenure();
        return (loan.getAmount() * monthlyRate * Math.pow(1 + monthlyRate, loan.getTenure()))
                / (Math.pow(1 + monthlyRate, loan.getTenure()) - 1);
    }

    private void validateJoiningDateChange(Employee existing, Employee updated) {
        if (updated.getJoiningDate().equals(existing.getJoiningDate())) return;
        if (updated.getJoiningDate().isBefore(existing.getJoiningDate())) return;

        Attendance first = attendance.findFirstByEmployeeIdOrderByYearAscMonthAscDayAsc(updated.getEmployeeId()).orElse(null);
        if (first == null) return;

        LocalDate firstAttendance = LocalDate.of(first.getYear(), first.getMonth(), first.getDay());
        if (firstAttendance.isBefore(updated.getJoiningDate())) {
            throw new IllegalArgumentException("Joining date cannot be moved after an existing attendance record.");
        }
    }

    private void normalize(Employee e) {
        if (e.getEmployeeId() != null) e.setEmployeeId(e.getEmployeeId().trim());
        if (e.getEmployeeName() != null) e.setEmployeeName(e.getEmployeeName().trim());
        if (e.getDepartment() != null) e.setDepartment(e.getDepartment().trim());
        if (e.getDesignation() != null) e.setDesignation(e.getDesignation().trim());
        if (e.getEmail() != null) e.setEmail(e.getEmail().trim().toLowerCase(Locale.ROOT));
        if (e.getPhone() != null) e.setPhone(e.getPhone().trim());
        if (e.getStatus() != null) e.setStatus(e.getStatus().trim());
        if (e.getDailyAttendanceStatus() == null) e.setDailyAttendanceStatus("NOT MARKED");
    }

    private void validate(Employee e) {
        if (e.getEmployeeId() == null || !ID.matcher(e.getEmployeeId()).matches()) {
            throw new IllegalArgumentException("Employee ID must start with E followed by 10-16 digits.");
        }
        if (e.getEmployeeName() == null || !NAME.matcher(e.getEmployeeName()).matches()) {
            throw new IllegalArgumentException("Name must be 2-100 letters, spaces, apostrophes, periods or hyphens.");
        }
        if (e.getAge() < 18 || e.getAge() > 100) {
            throw new IllegalArgumentException("Employee age must be between 18 and 100.");
        }
        if (e.getDepartment() == null || !DEPARTMENTS.contains(e.getDepartment())) {
            throw new IllegalArgumentException("Select a valid department.");
        }
        if (e.getDesignation() == null || !DESIGNATION.matcher(e.getDesignation()).matches()) {
            throw new IllegalArgumentException("Designation must be 2-100 valid characters.");
        }
        if (!Double.isFinite(e.getBaseSalary()) || e.getBaseSalary() <= 0) {
            throw new IllegalArgumentException("Monthly salary must be greater than zero.");
        }
        if (e.getEmail() == null || e.getEmail().length() > 150 || !EMAIL.matcher(e.getEmail()).matches()) {
            throw new IllegalArgumentException("Employee email must be a Gmail address ending exactly in @gmail.com.");
        }
        if (e.getPhone() == null || !PHONE.matcher(e.getPhone()).matches()) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits and start with 6, 7, 8 or 9.");
        }
        if (e.getJoiningDate() == null) throw new IllegalArgumentException("Joining date is required.");
        if (e.getJoiningDate().isAfter(LocalDate.now())) throw new IllegalArgumentException("Joining date cannot be after today.");
        if (!"Active".equals(e.getStatus()) && !"Inactive".equals(e.getStatus())) {
            throw new IllegalArgumentException("Employee status must be Active or Inactive.");
        }
        if (e.getLeaveBalance() < 0 || e.getLeaveBalance() > 365) {
            throw new IllegalArgumentException("Leave balance must be between 0 and 365 days.");
        }
    }
}
