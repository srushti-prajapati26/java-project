package com.employeemanagement.ems.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "employee_name", nullable = false, length = 100)
    private String employeeName;

    @Column(nullable = false)
    private int age;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String designation;

    @Column(name = "base_salary", nullable = false)
    private double baseSalary;

    @Column(name = "days_present", nullable = false)
    private int daysPresent = 0;

    @Column(name = "leave_balance", nullable = false)
    private int leaveBalance = 10;

    @Column(name = "loan_amount", nullable = false)
    private double loanAmount = 0.0;

    @Column(name = "daily_attendance_status", nullable = false, length = 30)
    private String dailyAttendanceStatus = "NOT MARKED";

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false, length = 30)
    private String status = "Active";

    public Employee() {}

    public Employee(String employeeId, String employeeName, int age,
                    String department, String designation, double baseSalary,
                    int leaveBalance) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.age = age;
        this.department = department;
        this.designation = designation;
        this.baseSalary = baseSalary;
        this.leaveBalance = leaveBalance;
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }
    public int getDaysPresent() { return daysPresent; }
    public void setDaysPresent(int daysPresent) { this.daysPresent = daysPresent; }
    public int getLeaveBalance() { return leaveBalance; }
    public void setLeaveBalance(int leaveBalance) { this.leaveBalance = leaveBalance; }
    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }
    public String getDailyAttendanceStatus() { return dailyAttendanceStatus; }
    public void setDailyAttendanceStatus(String dailyAttendanceStatus) { this.dailyAttendanceStatus = dailyAttendanceStatus; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public void markPresent() { dailyAttendanceStatus = "PRESENT"; }
    public void markAbsent() { dailyAttendanceStatus = "ABSENT"; }
    public void recordMonthlyAbsences(int daysAbsent) {
        if (daysAbsent >= 0 && daysAbsent <= 31) daysPresent = Math.max(0, 31 - daysAbsent);
    }
    public double calculateNetSalary() {
        if (daysPresent < 27) {
            double dailyRate = baseSalary / 30.0;
            return Math.max(0, baseSalary - ((27 - daysPresent) * dailyRate));
        }
        return baseSalary;
    }
    public void applyLeave(int days) { if (days > 0 && days <= leaveBalance) leaveBalance -= days; }
    public void takeLoan(double amount) { if (amount > 0) loanAmount += amount; }
}
