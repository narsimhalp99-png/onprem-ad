package com.amat.usermanagement.repository;

import com.amat.usermanagement.entity.RoleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleDefinition, Long> {
    Optional<RoleDefinition> findByRoleName(String roleName);
}
