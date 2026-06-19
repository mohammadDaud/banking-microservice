package com.bank.as.service;

import com.bank.as.model.entites.Role;
import com.bank.as.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public void initializeRoles() {

        createRoleIfNotExists("ROLE_CUSTOMER");
        createRoleIfNotExists("ROLE_ADMIN");
        createRoleIfNotExists("ROLE_MANAGER");
    }

    private void createRoleIfNotExists(String roleName) {

        roleRepository.findByRoleName(roleName)
                .orElseGet(() ->
                        roleRepository.save(
                                Role.builder()
                                        .id(UUID.randomUUID().toString())
                                        .roleName(roleName)
                                        .build()
                        ));
    }
}
