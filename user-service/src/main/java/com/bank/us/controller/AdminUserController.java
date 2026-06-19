package com.bank.us.controller;

import com.bank.us.dtos.UserProfileResponse;
import com.bank.us.dtos.UserResponse;
import com.bank.us.model.UserProfile;
import com.bank.us.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserProfileService service;

    @GetMapping
    public List<UserProfileResponse> getAllCustomers() {
        return service.getAllCustomers();
    }

    @GetMapping("/count")
    public Long count() {
        return service.count();
    }

    @GetMapping("/active-count")
    public Long activeCount() {
        return service.countByStatus("ACTIVE");
    }

    @GetMapping("/recent")
    public List<UserResponse> recentUsers() {
        return service.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
    }
}