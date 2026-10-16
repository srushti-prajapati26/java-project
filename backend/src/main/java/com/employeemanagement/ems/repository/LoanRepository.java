package com.employeemanagement.ems.repository;

import com.employeemanagement.ems.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findAllByOrderByIdDesc();
    List<Loan> findByEmployeeIdOrderByIdDesc(String employeeId);
    void deleteByEmployeeId(String employeeId);
}
