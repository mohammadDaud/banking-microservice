package com.bank.us.controller;

import com.bank.us.dtos.UserProfileRequest;
import com.bank.us.dtos.UserProfileResponse;
import com.bank.us.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "User Profile",
        description = "User Profile APIs"
)
public class UserProfileController {

    private final UserProfileService service;

    @Operation(summary = "Get User Profile")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String userId) {
        return ResponseEntity.ok(service.getProfile(userId));
    }

    @Operation(summary = "Update User Profile")
    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable String userId,
            @Valid
            @RequestBody UserProfileRequest request,
            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(service.updateProfile(userId, request, httpServletRequest));
    }
}