package com.employeemanagement.ems.model;

import jakarta.persistence.*;

@Entity
@Table(name = "employee_loan")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;

    @Column(nullable = false)
    private double amount;
    @Column(nullable = false)
    private double interest;
    @Column(nullable = false)
    private int tenure;
    @Column(nullable = false)
    private double outstanding;

    public Loan() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getInterest() { return interest; }
    public void setInterest(double interest) { this.interest = interest; }
    public int getTenure() { return tenure; }
    public void setTenure(int tenure) { this.tenure = tenure; }
    public double getOutstanding() { return outstanding; }
    public void setOutstanding(double outstanding) { this.outstanding = outstanding; }
}
