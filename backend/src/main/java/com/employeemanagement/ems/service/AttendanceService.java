package com.employeemanagement.ems.service;

import com.employeemanagement.ems.model.Attendance;
import com.employeemanagement.ems.model.Employee;
import com.employeemanagement.ems.repository.AttendanceRepository;
import com.employeemanagement.ems.repository.EmployeeRepository;
import com.employeemanagement.ems.util.WorkCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {
    private final AttendanceRepository repo;
    private final EmployeeRepository employees;

    public AttendanceService(AttendanceRepository repo, EmployeeRepository employees) {
        this.repo = repo;
        this.employees = employees;
    }

    public List<Attendance> current(int month, int year) {
        validateMonthYear(month, year);
        LocalDate today = LocalDate.now();
        Map<String, LocalDate> joiningDates = new HashMap<>();
        for (Employee employee : employees.findAll()) {
            // HashMap intentionally permits null dates for older records that predate joining-date enforcement.
            joiningDates.put(employee.getEmployeeId(), employee.getJoiningDate());
        }
        return repo.findByMonthAndYearOrderByDayAsc(month, year).stream()
                .filter(item -> {
                    try {
                        LocalDate date = LocalDate.of(item.getYear(), item.getMonth(), item.getDay());
                        LocalDate joiningDate = joiningDates.get(item.getEmployeeId());
                        // Do not surface malformed, pre-joining, weekend/holiday, or future records.
                        return !date.isAfter(today)
                                && (joiningDate == null || !date.isBefore(joiningDate))
                                && WorkCalendar.isWorkingDay(date);
                    } catch (DateTimeException ex) {
                        return false;
                    }
                })
                .toList();
    }

    @Transactional
    public Attendance save(Attendance request) {
        if (request == null) throw new IllegalArgumentException("Attendance data is required.");
        if (request.getEmployeeId() == null || request.getEmployeeId().isBlank()) {
            throw new IllegalArgumentException("Employee is required.");
        }
        Employee employee = employees.findById(request.getEmployeeId().trim())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
        if (!"Active".equals(employee.getStatus())) {
            throw new IllegalArgumentException("Attendance cannot be marked for an inactive employee.");
        }
        validateDate(request.getDay(), request.getMonth(), request.getYear(), employee);
        if (!"present".equals(request.getStatus()) && !"absent".equals(request.getStatus())) {
            throw new IllegalArgumentException("Attendance status must be present or absent.");
        }

        String employeeId = request.getEmployeeId().trim();
        Attendance existing = repo.findByEmployeeIdAndDayAndMonthAndYear(
                employeeId, request.getDay(), request.getMonth(), request.getYear()).orElseGet(Attendance::new);
        existing.setEmployeeId(employeeId);
        existing.setDay(request.getDay());
        existing.setMonth(request.getMonth());
        existing.setYear(request.getYear());
        existing.setStatus(request.getStatus());
        Attendance saved = repo.save(existing);
        syncEmployeeSummary(employee);
        return saved;
    }

    @Transactional
    public void delete(String employeeId, int day, int month, int year) {
        if (employeeId == null || employeeId.isBlank()) throw new IllegalArgumentException("Employee is required.");
        String normalizedId = employeeId.trim();
        Employee employee = employees.findById(normalizedId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
        validateDate(day, month, year, employee);
        repo.findByEmployeeIdAndDayAndMonthAndYear(normalizedId, day, month, year).ifPresent(repo::delete);
        syncEmployeeSummary(employee);
    }

    private void syncEmployeeSummary(Employee employee) {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();
        int presentDays = 0;
        for (Attendance item : repo.findByEmployeeIdAndMonthAndYear(employee.getEmployeeId(), month, year)) {
            try {
                LocalDate date = LocalDate.of(item.getYear(), item.getMonth(), item.getDay());
                if (!date.isAfter(today) && WorkCalendar.isWorkingDay(date) && "present".equals(item.getStatus())) {
                    presentDays++;
                }
            } catch (DateTimeException ignored) {
                // Ignore malformed legacy rows in the employee's attendance summary.
            }
        }
        employee.setDaysPresent(presentDays);
        employee.setDailyAttendanceStatus(
                WorkCalendar.isWorkingDay(today)
                        ? repo.findByEmployeeIdAndDayAndMonthAndYear(employee.getEmployeeId(), today.getDayOfMonth(), month, year)
                                .map(Attendance::getStatus)
                                .map(String::toUpperCase)
                                .orElse("NOT MARKED")
                        : "NOT MARKED"
        );
        employees.save(employee);
    }

    private void validateDate(int day, int month, int year, Employee employee) {
        validateMonthYear(month, year);
        LocalDate date;
        try {
            date = LocalDate.of(year, month, day);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Invalid calendar date.");
        }
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) throw new IllegalArgumentException("Future dates cannot be marked for attendance.");
        if (employee.getJoiningDate() != null && date.isBefore(employee.getJoiningDate())) {
            throw new IllegalArgumentException("Attendance cannot be marked before the employee's joining date.");
        }
        String holidayName = WorkCalendar.holidayName(date);
        if (!holidayName.isEmpty()) {
            throw new IllegalArgumentException("Attendance cannot be marked on a weekend or public holiday: " + holidayName + ".");
        }
    }

    private void validateMonthYear(int month, int year) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("Month must be between 1 and 12.");
        if (year < 2000 || year > 2100) throw new IllegalArgumentException("Invalid attendance year.");
        YearMonth.of(year, month);
    }
}
