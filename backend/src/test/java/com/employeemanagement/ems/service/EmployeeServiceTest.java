package com.employeemanagement.ems.service;

import com.employeemanagement.ems.model.Attendance;
import com.employeemanagement.ems.model.Employee;
import com.employeemanagement.ems.repository.AttendanceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock EmployeeRepository employees;
    @Mock AttendanceRepository attendance;
    @Mock LoanRepository loans;

    @Test
    void rejectsFutureJoiningDate() {
        EmployeeService service = new EmployeeService(employees, attendance, loans);
        Employee e = validEmployee();
        e.setJoiningDate(LocalDate.now().plusDays(1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(e));
        assertEquals("Joining date cannot be after today.", ex.getMessage());
        verifyNoInteractions(employees, attendance, loans);
    }

    @Test
    void rejectsInvalidPhoneNumber() {
        EmployeeService service = new EmployeeService(employees, attendance, loans);
        Employee e = validEmployee();
        e.setPhone("987654321");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(e));
        assertEquals("Phone number must be exactly 10 digits and start with 6, 7, 8 or 9.", ex.getMessage());
        verifyNoInteractions(employees, attendance, loans);
    }

    @Test
    void rejectsGmailAddressesThatDoNotEndExactlyInCom() {
        EmployeeService service = new EmployeeService(employees, attendance, loans);
        Employee e = validEmployee();
        e.setEmail("person@gmail.in");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(e));
        assertEquals("Employee email must be a Gmail address ending exactly in @gmail.com.", ex.getMessage());
        verifyNoInteractions(employees, attendance, loans);
    }

    @Test
    void rejectsInvalidDepartment() {
        EmployeeService service = new EmployeeService(employees, attendance, loans);
        Employee e = validEmployee();
        e.setDepartment("Unknown");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(e));
        assertEquals("Select a valid department.", ex.getMessage());
        verifyNoInteractions(employees, attendance, loans);
    }

    @Test
    void createsEmployeeWithNoFakeAttendance() {
        EmployeeService service = new EmployeeService(employees, attendance, loans);
        Employee e = validEmployee();
        when(employees.existsById("E1234567890")).thenReturn(false);
        when(employees.findByEmailIgnoreCase("a@gmail.com")).thenReturn(Optional.empty());
        when(employees.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee saved = service.add(e);
        assertEquals(0, saved.getDaysPresent());
        assertEquals("NOT MARKED", saved.getDailyAttendanceStatus());
        assertEquals(0, saved.getLoanAmount());
    }

    @Test
    void rejectsSalaryReductionThatMakesExistingLoansUnaffordable() {
        EmployeeService service = new EmployeeService(employees, attendance, loans);
        Employee existing = validEmployee();
        when(employees.findById("E1234567890")).thenReturn(Optional.of(existing));
        when(employees.findByEmailIgnoreCase("a@gmail.com")).thenReturn(Optional.of(existing));

        com.employeemanagement.ems.model.Loan loan = new com.employeemanagement.ems.model.Loan();
        loan.setId(1L);
        loan.setEmployeeId("E1234567890");
        loan.setAmount(360000);
        loan.setInterest(0);
        loan.setTenure(12);
        loan.setOutstanding(360000);
        when(loans.findByEmployeeIdOrderByIdDesc("E1234567890")).thenReturn(List.of(loan));

        Employee update = validEmployee();
        update.setBaseSalary(20000);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.update("E1234567890", update));
        assertEquals("Existing monthly loan EMI cannot exceed 50% of the employee's monthly salary.", ex.getMessage());
        verify(employees).findById("E1234567890");
        verify(loans).findByEmployeeIdOrderByIdDesc("E1234567890");
        verifyNoMoreInteractions(employees, loans, attendance);
    }

    @Test
    void rejectsMovingJoiningDatePastExistingAttendance() {
        EmployeeService service = new EmployeeService(employees, attendance, loans);
        Employee existing = validEmployee();
        existing.setJoiningDate(LocalDate.now().minusDays(10));
        when(employees.findById("E1234567890")).thenReturn(Optional.of(existing));
        when(employees.findByEmailIgnoreCase("a@gmail.com")).thenReturn(Optional.of(existing));

        Attendance first = new Attendance();
        first.setEmployeeId("E1234567890");
        LocalDate attendanceDate = LocalDate.now().minusDays(8);
        first.setDay(attendanceDate.getDayOfMonth());
        first.setMonth(attendanceDate.getMonthValue());
        first.setYear(attendanceDate.getYear());
        when(attendance.findFirstByEmployeeIdOrderByYearAscMonthAscDayAsc("E1234567890")).thenReturn(Optional.of(first));

        Employee update = validEmployee();
        update.setJoiningDate(LocalDate.now().minusDays(7));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.update("E1234567890", update));
        assertEquals("Joining date cannot be moved after an existing attendance record.", ex.getMessage());
        verify(employees).findById("E1234567890");
        verifyNoMoreInteractions(employees);
    }

    private Employee validEmployee() {
        Employee e = new Employee();
        e.setEmployeeId("E1234567890");
        e.setEmployeeName("Test Employee");
        e.setAge(25);
        e.setDepartment("Technology");
        e.setDesignation("Developer");
        e.setBaseSalary(50000);
        e.setEmail("a@gmail.com");
        e.setPhone("9876543210");
        e.setJoiningDate(LocalDate.now());
        e.setStatus("Active");
        e.setLeaveBalance(10);
        return e;
    }
}
