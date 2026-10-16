package com.employeemanagement.ems.service;

import com.employeemanagement.ems.model.Employee;
import com.employeemanagement.ems.model.Loan;
import com.employeemanagement.ems.repository.EmployeeRepository;
import com.employeemanagement.ems.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {
    @Mock LoanRepository loans;
    @Mock EmployeeRepository employees;

    @Test
    void rejectsLoansForInactiveEmployees() {
        LoanService service = new LoanService(loans, employees);
        Employee e = employee("Inactive");
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));

        Loan loan = loan(10000, 8, 12, 10000);
        assertThrows(IllegalArgumentException.class, () -> service.create(loan));
        verifyNoInteractions(loans);
    }

    @Test
    void rejectsLoanWhenCombinedEmiExceedsHalfSalary() {
        LoanService service = new LoanService(loans, employees);
        Employee e = employee("Active");
        e.setBaseSalary(50000);
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));

        Loan existing = loan(240000, 0, 12, 240000);
        existing.setId(1L);
        when(loans.findByEmployeeIdOrderByIdDesc("E1234567890")).thenReturn(List.of(existing));

        // The new principal is within the salary cap, but its 1-month EMI pushes the combined EMI over 50%.
        Loan newLoan = loan(5000, 100, 1, 5000);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(newLoan));
        assertEquals("Total monthly loan EMI cannot exceed 50% of the employee's monthly salary.", ex.getMessage());
        verify(loans, never()).save(any(Loan.class));
    }

    @Test
    void rejectsChangingLoanEmployee() {
        LoanService service = new LoanService(loans, employees);
        Loan existing = loan(10000, 8, 12, 10000);
        existing.setId(10L);
        existing.setEmployeeId("E1234567890");
        when(loans.findById(10L)).thenReturn(Optional.of(existing));

        Loan update = loan(10000, 8, 12, 10000);
        update.setEmployeeId("E1234567891");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.update(10L, update));
        assertEquals("Loan employee cannot be changed.", ex.getMessage());
        verify(employees, never()).findById(anyString());
        verify(loans, never()).save(any(Loan.class));
    }

    @Test
    void rejectsLoanPrincipalAboveHalfMonthlySalary() {
        LoanService service = new LoanService(loans, employees);
        Employee e = employee("Active");
        e.setBaseSalary(50000);
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));

        Loan loan = loan(25000.01, 0, 12, 25000.01);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(loan));
        assertEquals("Loan principal cannot exceed 50% of the employee's monthly salary.", ex.getMessage());
        verify(loans, never()).save(any(Loan.class));
    }

    @Test
    void rejectsLoanWithUnaffordableEmi() {
        LoanService service = new LoanService(loans, employees);
        Employee e = employee("Active");
        e.setBaseSalary(10000);
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));

        Loan loan = loan(5000, 100, 1, 5000);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(loan));
        assertEquals("Total monthly loan EMI cannot exceed 50% of the employee's monthly salary.", ex.getMessage());
        verify(loans, never()).save(any(Loan.class));
    }

    @Test
    void recordsPaymentAndSyncsEmployeeLoanAmount() {
        LoanService service = new LoanService(loans, employees);
        Employee e = employee("Active");
        Loan existing = loan(120000, 8, 12, 100000);
        existing.setId(7L);
        when(loans.findById(7L)).thenReturn(Optional.of(existing));
        when(loans.findByEmployeeIdOrderByIdDesc("E1234567890")).thenReturn(List.of(existing));
        when(loans.save(existing)).thenReturn(existing);
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));
        when(employees.save(e)).thenReturn(e);

        Loan updated = service.recordPayment(7L, 25000);

        assertEquals(75000, updated.getOutstanding(), 0.000001);
        verify(loans).save(existing);
        verify(employees).save(e);
        assertEquals(75000, e.getLoanAmount(), 0.000001);
    }

    private Loan loan(double amount, double interest, int tenure, double outstanding) {
        Loan loan = new Loan();
        loan.setEmployeeId("E1234567890");
        loan.setAmount(amount);
        loan.setInterest(interest);
        loan.setTenure(tenure);
        loan.setOutstanding(outstanding);
        return loan;
    }

    private Employee employee(String status) {
        Employee e = new Employee();
        e.setEmployeeId("E1234567890");
        e.setEmployeeName("Test Employee");
        e.setAge(25);
        e.setDepartment("Technology");
        e.setDesignation("Developer");
        e.setBaseSalary(50000);
        e.setEmail("a@gmail.com");
        e.setPhone("9876543210");
        e.setJoiningDate(LocalDate.now().minusDays(10));
        e.setStatus(status);
        e.setLeaveBalance(10);
        return e;
    }
}
