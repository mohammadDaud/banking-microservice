package com.bank.us.controller;

import com.bank.us.dtos.*;
import com.bank.us.enums.UserStatus;
import com.bank.us.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Admin Customer Management",
        description = "Admin APIs for customer management"
)
public class AdminUserController {

    private final UserProfileService service;

    @Operation(summary = "Get all customers")
    @GetMapping
    public List<UserProfileResponse> getAllCustomers() {
        return service.getAllCustomers();
    }

    @Operation(summary = "Get paginated customers")
    @GetMapping("/page")
    public PageResponse<UserProfileResponse> getCustomers(UserProfileSearchRequest request) {
        return service.getCustomers(request);
    }

    @Operation(summary = "Delete customer")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deleteCustomer(@PathVariable String userId, HttpServletRequest request) {
        return ResponseEntity.ok(service.deleteCustomer(userId, request));
    }

    @Operation(summary = "Restore deleted customer")
    @PatchMapping("/{userId}/restore")
    public ResponseEntity<ApiResponse> restoreCustomer(@PathVariable String userId, HttpServletRequest request) {
        return ResponseEntity.ok(service.restoreCustomer(userId, request));
    }

    @Operation(summary = "Get customer details")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getCustomer(@PathVariable String userId) {
        return ResponseEntity.ok(service.getCustomer(userId));
    }

    @Operation(summary = "Total customers")
    @GetMapping("/count")
    public Long count() {
        return service.count();
    }

    @Operation(summary = "Active customers")
    @GetMapping("/active-count")
    public Long activeCount() {
        return service.countByStatus(UserStatus.ACTIVE);
    }

    @Operation(summary = "Recent customers")
    @GetMapping("/recent")
    public List<UserResponse> recentUsers() {
        return service.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
    }

    @Operation(summary = "Dashboard statistics")
    @GetMapping("/dashboard/stats")
    public DashboardStatsResponse dashboardStats() {
        return service.getDashboardStats();
    }
}