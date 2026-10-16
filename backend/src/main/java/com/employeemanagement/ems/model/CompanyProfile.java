package com.employeemanagement.ems.model;

import jakarta.persistence.*;

@Entity
@Table(name = "company_profile")
public class CompanyProfile {
    @Id
    private Long id = 1L;
    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName = "EMS";
    @Column(length = 150)
    private String email = "admin@ems.com";
    @Column(length = 50)
    private String phone = "+91 98765 43210";
    @Column(name = "system_name", nullable = false, length = 150)
    private String systemName = "Employee Management System";

    public CompanyProfile() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }
}
