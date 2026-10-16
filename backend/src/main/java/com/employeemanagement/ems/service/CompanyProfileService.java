package com.employeemanagement.ems.service;

import com.employeemanagement.ems.model.CompanyProfile;
import com.employeemanagement.ems.repository.CompanyProfileRepository;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class CompanyProfileService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern NAME = Pattern.compile("^[\\p{L}0-9][\\p{L}0-9 &().,'-]{1,149}$");
    private static final Pattern PHONE = Pattern.compile("^(?=(?:\\D*\\d){7,15}\\D*$)[0-9+()\\-\\s]{7,20}$");

    private final CompanyProfileRepository repo;
    public CompanyProfileService(CompanyProfileRepository repo) { this.repo = repo; }

    public CompanyProfile get() { return repo.findById(1L).orElseGet(() -> repo.save(new CompanyProfile())); }

    public CompanyProfile save(CompanyProfile input) {
        if (input == null) throw new IllegalArgumentException("Company profile data is required.");
        String companyName = input.getCompanyName() == null ? "" : input.getCompanyName().trim();
        String systemName = input.getSystemName() == null ? "" : input.getSystemName().trim();
        String email = input.getEmail() == null ? "" : input.getEmail().trim();
        String phone = input.getPhone() == null ? "" : input.getPhone().trim();

        if (companyName.length() < 2 || companyName.length() > 150 || !NAME.matcher(companyName).matches()) throw new IllegalArgumentException("Company name must be 2-150 valid characters.");
        if (systemName.length() < 2 || systemName.length() > 150 || !NAME.matcher(systemName).matches()) throw new IllegalArgumentException("System name must be 2-150 valid characters.");
        if (email.length() > 150 || (!email.isBlank() && !EMAIL.matcher(email).matches())) throw new IllegalArgumentException("Enter a valid company email address.");
        if (phone.length() > 50 || (!phone.isBlank() && !PHONE.matcher(phone).matches())) throw new IllegalArgumentException("Enter a valid company phone number.");

        input.setId(1L);
        input.setCompanyName(companyName);
        input.setSystemName(systemName);
        input.setEmail(email);
        input.setPhone(phone);
        return repo.save(input);
    }
}
