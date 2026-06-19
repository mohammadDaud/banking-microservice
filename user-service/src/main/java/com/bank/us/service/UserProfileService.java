package com.bank.us.service;

import com.bank.us.dtos.UserProfileRequest;
import com.bank.us.dtos.UserProfileResponse;
import com.bank.us.dtos.UserResponse;
import com.bank.us.enums.UserStatus;
import com.bank.us.model.UserProfile;
import com.bank.us.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfile getProfile(String userId) {
        return repository.findById(userId).orElseThrow();
    }

    public UserProfile updateProfile(String userId,UserProfileRequest request) {
        UserProfile profile = getProfile(userId);
        profile.setFirstName(request.getFirstName());
        profile.setMiddleName(request.getMiddleName());
        profile.setLastName(request.getLastName());
        profile.setMobileNumber(request.getMobileNumber());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setNationality(request.getNationality());
        profile.setMaritalStatus(request.getMaritalStatus());
        profile.setStatus(UserStatus.ACTIVE.name());
        profile.setUpdatedAt(LocalDateTime.now());
        return repository.save(profile);
    }

    public List<UserProfileResponse> getAllCustomers() {
        return repository
                .findAll()
                .stream()
                .map(this::map)
                .toList();
    }
    public Long count() {
        return repository.count();
    }

    public Long countByStatus(String active) {
        return repository.countByStatus(active);
    }

    public List<UserResponse> findAllByOrderByCreatedAtDesc(PageRequest of) {
        Pageable pageable = PageRequest.of(of.getPageNumber(), of.getPageSize());
        List<UserProfile> userProfile= repository.findAllByOrderByCreatedAtDesc(pageable);
        return userProfile.stream()
                .map(this::mapUser)
                .toList();
    }


    private UserProfileResponse map(
            UserProfile profile) {

        return UserProfileResponse
                .builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .middleName(profile.getMiddleName())
                .lastName(profile.getLastName())
                .mobileNumber(profile.getMobileNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .nationality(profile.getNationality())
                .maritalStatus(profile.getMaritalStatus())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private UserResponse mapUser(UserProfile profile) {
        return UserResponse
                .builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .middleName(profile.getMiddleName())
                .lastName(profile.getLastName())
                .mobileNumber(profile.getMobileNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .nationality(profile.getNationality())
                .maritalStatus(profile.getMaritalStatus())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }


}