package com.employeemanagement.ems.controller;

import com.employeemanagement.ems.model.Employee;
import com.employeemanagement.ems.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmployeeController {
    private final EmployeeService service;
    public EmployeeController(EmployeeService service) { this.service = service; }

    @GetMapping
    public List<Employee> all() { return service.all(); }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> one(@PathVariable String id) {
        Employee employee = service.one(id);
        return employee == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(employee);
    }

    @PostMapping
    public ResponseEntity<Employee> add(@RequestBody Employee employee) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable String id, @RequestBody Employee employee) {
        Employee updated = service.update(id, employee);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
