package com.ldap.userenrollment.repository;

import com.ldap.userenrollment.entity.RoleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleDefinition, Long> {
    Optional<RoleDefinition> findByRoleName(String roleName);
}
