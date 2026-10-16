package com.employeemanagement.ems.service;

import com.employeemanagement.ems.model.Attendance;
import com.employeemanagement.ems.model.Employee;
import com.employeemanagement.ems.repository.AttendanceRepository;
import com.employeemanagement.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
    @Mock AttendanceRepository attendance;
    @Mock EmployeeRepository employees;

    @Test
    void rejectsAttendanceBeforeJoiningDate() {
        AttendanceService service = new AttendanceService(attendance, employees);
        Employee e = employee("Active");
        e.setJoiningDate(LocalDate.now().minusDays(5));
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));

        LocalDate beforeJoin = e.getJoiningDate().minusDays(1);
        Attendance a = request("E1234567890", beforeJoin);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(a));
        assertEquals("Attendance cannot be marked before the employee's joining date.", ex.getMessage());
        verifyNoInteractions(attendance);
    }

    @Test
    void rejectsFutureAttendance() {
        AttendanceService service = new AttendanceService(attendance, employees);
        Employee e = employee("Active");
        e.setJoiningDate(LocalDate.now().minusDays(5));
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));

        LocalDate future = LocalDate.now().plusDays(1);
        Attendance a = request("E1234567890", future);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(a));
        assertEquals("Future dates cannot be marked for attendance.", ex.getMessage());
        verifyNoInteractions(attendance);
    }

    @Test
    void rejectsAttendanceOnWeekend() {
        AttendanceService service = new AttendanceService(attendance, employees);
        Employee e = employee("Active");
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        e.setJoiningDate(saturday.minusDays(10));
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));

        Attendance a = request("E1234567890", saturday);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(a));
        assertTrue(ex.getMessage().contains("weekend or public holiday"));
        verifyNoInteractions(attendance);
    }

    @Test
    void rejectsAttendanceForInactiveEmployee() {
        AttendanceService service = new AttendanceService(attendance, employees);
        Employee e = employee("Inactive");
        when(employees.findById("E1234567890")).thenReturn(Optional.of(e));

        Attendance a = request("E1234567890", LocalDate.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(a));
        assertEquals("Attendance cannot be marked for an inactive employee.", ex.getMessage());
        verifyNoInteractions(attendance);
    }

    private Attendance request(String id, LocalDate date) {
        Attendance a = new Attendance();
        a.setEmployeeId(id);
        a.setDay(date.getDayOfMonth());
        a.setMonth(date.getMonthValue());
        a.setYear(date.getYear());
        a.setStatus("present");
        return a;
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
