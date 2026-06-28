package com.bank.us.controller;

import com.bank.us.dtos.UserProfileRequest;
import com.bank.us.model.UserProfile;
import com.bank.us.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService service;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getProfile(@PathVariable String userId) {
        return ResponseEntity.ok(service.getProfile(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfile> updateProfile(
            @PathVariable String userId,
            @RequestBody UserProfileRequest request,
            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(service.updateProfile(userId,request,httpServletRequest));
    }
}