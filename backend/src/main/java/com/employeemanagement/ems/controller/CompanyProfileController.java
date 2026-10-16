package com.employeemanagement.ems.controller;

import com.employeemanagement.ems.model.CompanyProfile;
import com.employeemanagement.ems.service.CompanyProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company-profile")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class CompanyProfileController {
    private final CompanyProfileService service;
    public CompanyProfileController(CompanyProfileService service) { this.service = service; }

    @GetMapping
    public CompanyProfile get() { return service.get(); }

    @PutMapping
    public ResponseEntity<CompanyProfile> save(@RequestBody CompanyProfile profile) {
        return ResponseEntity.ok(service.save(profile));
    }
}
