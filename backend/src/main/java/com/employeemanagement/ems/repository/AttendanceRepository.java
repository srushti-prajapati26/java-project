package com.employeemanagement.ems.repository;

import com.employeemanagement.ems.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByMonthAndYearOrderByDayAsc(int month, int year);
    Optional<Attendance> findByEmployeeIdAndDayAndMonthAndYear(String employeeId, int day, int month, int year);
    void deleteByEmployeeId(String employeeId);
    List<Attendance> findByEmployeeIdAndMonthAndYear(String employeeId, int month, int year);
    java.util.Optional<Attendance> findFirstByEmployeeIdOrderByYearAscMonthAscDayAsc(String employeeId);
}
