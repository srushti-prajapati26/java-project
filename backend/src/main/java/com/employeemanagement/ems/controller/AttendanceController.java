package com.employeemanagement.ems.controller;

import com.employeemanagement.ems.model.Attendance;
import com.employeemanagement.ems.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AttendanceController {
    private final AttendanceService service;
    public AttendanceController(AttendanceService service) { this.service = service; }

    @GetMapping
    public List<Attendance> current(@RequestParam int month, @RequestParam int year) {
        return service.current(month, year);
    }

    @PostMapping
    public ResponseEntity<Attendance> save(@RequestBody Attendance attendance) {
        return ResponseEntity.ok(service.save(attendance));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam String employeeId,
                                       @RequestParam int day,
                                       @RequestParam int month,
                                       @RequestParam int year) {
        service.delete(employeeId, day, month, year);
        return ResponseEntity.noContent().build();
    }
}
