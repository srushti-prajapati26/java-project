package com.employeemanagement.ems.controller;

import com.employeemanagement.ems.model.Loan;
import com.employeemanagement.ems.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class LoanController {
    private final LoanService service;
    public LoanController(LoanService service) { this.service = service; }

    @GetMapping
    public List<Loan> all() { return service.all(); }

    @PostMapping
    public ResponseEntity<Loan> create(@RequestBody Loan loan) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(loan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Loan> update(@PathVariable Long id, @RequestBody Loan loan) {
        Loan updated = service.update(id, loan);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<Loan> payment(@PathVariable Long id, @RequestParam double amount) {
        return ResponseEntity.ok(service.recordPayment(id, amount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
