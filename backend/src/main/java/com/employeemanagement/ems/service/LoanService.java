package com.employeemanagement.ems.service;

import com.employeemanagement.ems.model.Employee;
import com.employeemanagement.ems.model.Loan;
import com.employeemanagement.ems.repository.EmployeeRepository;
import com.employeemanagement.ems.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {
    private static final double MAX_EMI_TO_SALARY_RATIO = 0.50;

    private final LoanRepository repo;
    private final EmployeeRepository employees;

    public LoanService(LoanRepository repo, EmployeeRepository employees) {
        this.repo = repo;
        this.employees = employees;
    }

    public List<Loan> all() { return repo.findAllByOrderByIdDesc(); }

    @Transactional
    public Loan create(Loan loan) {
        if (loan == null) throw new IllegalArgumentException("Loan data is required.");
        Employee employee = validate(loan);
        loan.setEmployeeId(loan.getEmployeeId().trim());
        loan.setOutstanding(loan.getAmount());
        validateAffordability(loan, employee, null);
        Loan saved = repo.save(loan);
        syncEmployeeLoanAmount(employee);
        return saved;
    }

    @Transactional
    public Loan update(Long id, Loan update) {
        if (update == null) throw new IllegalArgumentException("Loan data is required.");
        Loan existing = repo.findById(id).orElse(null);
        if (existing == null) return null;
        if (update.getEmployeeId() == null || update.getEmployeeId().isBlank()) {
            throw new IllegalArgumentException("Employee is required.");
        }
        update.setEmployeeId(update.getEmployeeId().trim());
        if (!existing.getEmployeeId().equals(update.getEmployeeId())) {
            throw new IllegalArgumentException("Loan employee cannot be changed.");
        }
        Employee employee = validate(update);
        validateAffordability(update, employee, id);
        existing.setEmployeeId(update.getEmployeeId());
        existing.setAmount(update.getAmount());
        existing.setInterest(update.getInterest());
        existing.setTenure(update.getTenure());
        existing.setOutstanding(update.getOutstanding());
        Loan saved = repo.save(existing);
        syncEmployeeLoanAmount(employee);
        return saved;
    }

    @Transactional
    public Loan recordPayment(Long id, double paymentAmount) {
        if (!Double.isFinite(paymentAmount) || paymentAmount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        Loan loan = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found."));

        double outstanding = loan.getOutstanding();
        if (outstanding <= 0) {
            throw new IllegalArgumentException("This loan is already fully paid.");
        }
        if (paymentAmount > outstanding + 0.000001) {
            throw new IllegalArgumentException("Payment cannot exceed the outstanding loan amount.");
        }

        loan.setOutstanding(Math.max(0, outstanding - paymentAmount));
        Loan saved = repo.save(loan);
        employees.findById(loan.getEmployeeId()).ifPresent(this::syncEmployeeLoanAmount);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Loan existing = repo.findById(id).orElse(null);
        if (existing == null) throw new IllegalArgumentException("Loan not found.");
        repo.deleteById(id);
        employees.findById(existing.getEmployeeId()).ifPresent(this::syncEmployeeLoanAmount);
    }

    private Employee validate(Loan l) {
        if (l.getEmployeeId() == null || l.getEmployeeId().isBlank()) {
            throw new IllegalArgumentException("Employee is required.");
        }
        Employee employee = employees.findById(l.getEmployeeId().trim())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
        if ("Inactive".equals(employee.getStatus())) {
            throw new IllegalArgumentException("A new loan can only be created for an active employee.");
        }
        if (employee.getJoiningDate() != null && employee.getJoiningDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Employee has not joined yet.");
        }
        if (!Double.isFinite(l.getAmount()) || l.getAmount() <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero.");
        }
        double maxPrincipal = employee.getBaseSalary() * MAX_EMI_TO_SALARY_RATIO;
        if (l.getAmount() > maxPrincipal + 0.000001) {
            throw new IllegalArgumentException("Loan principal cannot exceed 50% of the employee's monthly salary.");
        }
        if (!Double.isFinite(l.getInterest()) || l.getInterest() < 0 || l.getInterest() > 100) {
            throw new IllegalArgumentException("Interest must be between 0 and 100%.");
        }
        if (l.getTenure() < 1 || l.getTenure() > 120) {
            throw new IllegalArgumentException("Loan tenure must be between 1 and 120 months.");
        }
        if (!Double.isFinite(l.getOutstanding()) || l.getOutstanding() < 0 || l.getOutstanding() > l.getAmount()) {
            throw new IllegalArgumentException("Outstanding amount must be between 0 and the loan amount.");
        }
        return employee;
    }

    private void validateAffordability(Loan loan, Employee employee, Long excludedLoanId) {
        double newEmi = loan.getOutstanding() > 0 ? calculateEmi(loan) : 0;
        double existingEmi = repo.findByEmployeeIdOrderByIdDesc(employee.getEmployeeId()).stream()
                .filter(existing -> existing.getOutstanding() > 0)
                .filter(existing -> excludedLoanId == null || !excludedLoanId.equals(existing.getId()))
                .mapToDouble(this::calculateEmi)
                .sum();
        double maximum = employee.getBaseSalary() * MAX_EMI_TO_SALARY_RATIO;
        if (!Double.isFinite(newEmi) || !Double.isFinite(existingEmi) || existingEmi + newEmi > maximum + 0.000001) {
            throw new IllegalArgumentException("Total monthly loan EMI cannot exceed 50% of the employee's monthly salary.");
        }
    }

    private void syncEmployeeLoanAmount(Employee employee) {
        double totalOutstanding = repo.findByEmployeeIdOrderByIdDesc(employee.getEmployeeId()).stream()
                .mapToDouble(Loan::getOutstanding)
                .sum();
        employee.setLoanAmount(totalOutstanding);
        employees.save(employee);
    }

    private double calculateEmi(Loan loan) {
        double monthlyRate = loan.getInterest() / 12.0 / 100.0;
        if (monthlyRate == 0) return loan.getAmount() / loan.getTenure();
        return (loan.getAmount() * monthlyRate * Math.pow(1 + monthlyRate, loan.getTenure()))
                / (Math.pow(1 + monthlyRate, loan.getTenure()) - 1);
    }
}
