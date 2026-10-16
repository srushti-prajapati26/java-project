package com.employeemanagement.ems.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;

/**
 * Working-day calendar used by attendance and payroll.
 * The 2026 list reflects Maharashtra's published public-holiday calendar.
 * Refresh moving/religious dates when the government publishes the next year's list.
 */
public final class WorkCalendar {
    private static final Map<MonthDay, String> ANNUAL_HOLIDAYS = Map.of(
            MonthDay.of(1, 26), "Republic Day",
            MonthDay.of(5, 1), "Maharashtra Day",
            MonthDay.of(8, 15), "Independence Day",
            MonthDay.of(10, 2), "Mahatma Gandhi Jayanti"
    );

    private static final Map<LocalDate, String> MAHARASHTRA_HOLIDAYS_2026 = Map.ofEntries(
            Map.entry(LocalDate.of(2026, 1, 26), "Republic Day"),
            Map.entry(LocalDate.of(2026, 2, 15), "Mahashivratri"),
            Map.entry(LocalDate.of(2026, 2, 19), "Chhatrapati Shivaji Maharaj Jayanti"),
            Map.entry(LocalDate.of(2026, 3, 3), "Holi (Second Day)"),
            Map.entry(LocalDate.of(2026, 3, 19), "Gudhi Padwa"),
            Map.entry(LocalDate.of(2026, 3, 21), "Ramzan-Id"),
            Map.entry(LocalDate.of(2026, 3, 26), "Ram Navami"),
            Map.entry(LocalDate.of(2026, 3, 31), "Mahavir Janmakalyanak"),
            Map.entry(LocalDate.of(2026, 4, 3), "Good Friday"),
            Map.entry(LocalDate.of(2026, 4, 14), "Dr. Babasaheb Ambedkar Jayanti"),
            Map.entry(LocalDate.of(2026, 5, 1), "Maharashtra Day / Buddha Pournima"),
            Map.entry(LocalDate.of(2026, 5, 28), "Bakri Id"),
            Map.entry(LocalDate.of(2026, 6, 26), "Moharum"),
            Map.entry(LocalDate.of(2026, 8, 15), "Independence Day / Parsi New Year"),
            Map.entry(LocalDate.of(2026, 8, 26), "Id-E-Milad"),
            Map.entry(LocalDate.of(2026, 9, 14), "Ganesh Chaturthi"),
            Map.entry(LocalDate.of(2026, 10, 2), "Mahatma Gandhi Jayanti"),
            Map.entry(LocalDate.of(2026, 10, 20), "Dasara"),
            Map.entry(LocalDate.of(2026, 11, 8), "Diwali Amavasya (Laxmi Pujan)"),
            Map.entry(LocalDate.of(2026, 11, 10), "Diwali (Bali Pratipada)"),
            Map.entry(LocalDate.of(2026, 11, 24), "Guru Nanak Jayanti"),
            Map.entry(LocalDate.of(2026, 12, 25), "Christmas Day")
    );

    private WorkCalendar() {}

    /** Returns a holiday name, or an empty string if the date is a working day. */
    public static String holidayName(LocalDate date) {
        if (date == null) return "";

        String specificHoliday = MAHARASHTRA_HOLIDAYS_2026.get(date);
        if (specificHoliday != null) return specificHoliday;

        String annualHoliday = ANNUAL_HOLIDAYS.get(MonthDay.from(date));
        if (annualHoliday != null) return annualHoliday;

        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY) return "Saturday";
        if (day == DayOfWeek.SUNDAY) return "Sunday";
        return "";
    }

    public static boolean isWorkingDay(LocalDate date) {
        return holidayName(date).isEmpty();
    }
}
