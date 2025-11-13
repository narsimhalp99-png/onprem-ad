package com.ldap.userenrollment.service;

import com.ldap.userenrollment.entity.RoleDefinition;
import com.ldap.userenrollment.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleService {
    private final RoleRepository repo;

    public RoleService(RoleRepository repo) { this.repo = repo; }

    public Optional<RoleDefinition> findById(Long id) { return repo.findById(id); }
    public Optional<RoleDefinition> findByName(String name) { return repo.findByRoleName(name); }
}
