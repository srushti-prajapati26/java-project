package com.employeemanagement.ems.model;

import jakarta.persistence.*;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "day", "month", "year"}))
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;
    @Column(nullable = false)
    private int day;
    @Column(nullable = false)
    private int month;
    @Column(nullable = false)
    private int year;
    @Column(nullable = false, length = 20)
    private String status;

    public Attendance() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
