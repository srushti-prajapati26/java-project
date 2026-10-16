package com.employeemanagement.ems.repository;

import com.employeemanagement.ems.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByEmailIgnoreCase(String email);
}
