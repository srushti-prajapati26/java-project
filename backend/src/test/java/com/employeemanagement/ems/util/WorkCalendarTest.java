package com.employeemanagement.ems.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class WorkCalendarTest {
    @Test
    void marksSaturdayAndSundayAsHolidays() {
        assertEquals("Saturday", WorkCalendar.holidayName(LocalDate.of(2026, 10, 3)));
        assertEquals("Sunday", WorkCalendar.holidayName(LocalDate.of(2026, 10, 4)));
        assertFalse(WorkCalendar.isWorkingDay(LocalDate.of(2026, 10, 3)));
    }

    @Test
    void marksMaharashtraPublicHolidaysAsHolidays() {
        assertEquals("Mahatma Gandhi Jayanti", WorkCalendar.holidayName(LocalDate.of(2026, 10, 2)));
        assertEquals("Dasara", WorkCalendar.holidayName(LocalDate.of(2026, 10, 20)));
        assertFalse(WorkCalendar.isWorkingDay(LocalDate.of(2026, 10, 20)));
    }

    @Test
    void ordinaryWeekdayIsAWorkingDay() {
        assertEquals("", WorkCalendar.holidayName(LocalDate.of(2026, 10, 26)));
        assertTrue(WorkCalendar.isWorkingDay(LocalDate.of(2026, 10, 26)));
    }
}
