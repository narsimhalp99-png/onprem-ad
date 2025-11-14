package com.ldap.userenrollment.repository;

import com.ldap.userenrollment.entity.RoleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleDefinition, Long> {
    Optional<RoleDefinition> findByRoleName(String roleName);
}
