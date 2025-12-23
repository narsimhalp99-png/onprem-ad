package com.amat.accessmanagement.service;

import com.amat.accessmanagement.dto.AssignRoleRequest;
import com.amat.accessmanagement.entity.RoleDefinition;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.entity.UserRoleMapping;
import com.amat.accessmanagement.repository.RoleRepository;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.accessmanagement.repository.UserRoleMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class RoleService {

    @Autowired
    UserEnrollmentRepository userRepo;

    @Autowired
    RoleRepository roleRepo;

    @Autowired
    UserRoleMappingRepository userRoleRepo;

    @Transactional
    public UserRoleMapping assignRole(String employeeId, AssignRoleRequest req) {

        log.info(
                "START assignRole | employeeId={} | roleId={} | isRoleActive={}",
                employeeId,
                req.getRoleId(),
                req.getIsRoleActive()
        );

        if (employeeId == null) {
            log.error("Invalid input: employeeId is null");
            throw new IllegalArgumentException("EmployeeId must not be null");
        }

        if (req.getRoleId() == null || req.getRoleId().isBlank()) {
            log.error("Invalid input: roleId is null or blank | employeeId={}", employeeId);
            throw new IllegalArgumentException("AssigningRoleId must not be null or empty");
        }

        String roleId = req.getRoleId();
        Boolean isActive = req.getIsRoleActive() == null ? Boolean.TRUE : req.getIsRoleActive();

        log.debug(
                "Resolved assignRole inputs | employeeId={} | roleId={} | isActive={}",
                employeeId,
                roleId,
                isActive
        );

        UserEntity userEntity = userRepo.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("User not found while assigning role | employeeId={}", employeeId);
                    return new RuntimeException("User not found: " + employeeId);
                });

        log.debug("User entity fetched | employeeId={}", employeeId);

        UserRoleMapping existing =
                userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);

        if (existing != null) {

            log.info(
                    "Existing role mapping found | employeeId={} | roleId={} | currentStatus={}",
                    employeeId,
                    roleId,
                    existing.getAssignedRoleStatus()
            );

            // Idempotent update
            if (!Objects.equals(existing.getAssignedRoleStatus(), isActive)) {

                log.info(
                        "Updating role status | employeeId={} | roleId={} | newStatus={}",
                        employeeId,
                        roleId,
                        isActive
                );

                existing.setAssignedRoleStatus(isActive);
                UserRoleMapping updated = userRoleRepo.save(existing);

                log.info(
                        "Role status updated successfully | employeeId={} | roleId={}",
                        employeeId,
                        roleId
                );

                return updated;
            }

            log.info(
                    "No role update required (idempotent) | employeeId={} | roleId={}",
                    employeeId,
                    roleId
            );

            return existing;
        }

        UserRoleMapping mapping = new UserRoleMapping();
        mapping.setUser(userEntity);           // FK
        mapping.setAssignedRoleId(roleId);
        mapping.setAssignedRoleStatus(isActive);

        log.info(
                "Creating new role mapping | employeeId={} | roleId={} | isActive={}",
                employeeId,
                roleId,
                isActive
        );

        try {
            UserRoleMapping saved = userRoleRepo.save(mapping);

            log.info(
                    "Role assigned successfully | employeeId={} | roleId={}",
                    employeeId,
                    roleId
            );

            return saved;

        } catch (DataIntegrityViolationException ex) {

            log.warn(
                    "DataIntegrityViolation while assigning role (possible race condition) | employeeId={} | roleId={}",
                    employeeId,
                    roleId,
                    ex
            );

            // If unique constraint violated due to race condition, return existing
            UserRoleMapping already =
                    userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);

            if (already != null) {
                log.info(
                        "Recovered existing role mapping after constraint violation | employeeId={} | roleId={}",
                        employeeId,
                        roleId
                );
                return already;
            }

            log.error(
                    "Role assignment failed irrecoverably | employeeId={} | roleId={}",
                    employeeId,
                    roleId,
                    ex
            );

            throw ex;
        }
    }

    @Transactional
    public void revokeRole(String employeeId, String roleId) {

        log.info(
                "START revokeRole | employeeId={} | roleId={}",
                employeeId,
                roleId
        );

        UserRoleMapping existing =
                userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);

        if (existing == null) {
            log.error(
                    "Cannot revoke role: role not assigned | employeeId={} | roleId={}",
                    employeeId,
                    roleId
            );
            throw new RuntimeException("Role not assigned to this user");
        }

        existing.setAssignedRoleStatus(false);
        userRoleRepo.save(existing);

        log.info(
                "Role revoked successfully | employeeId={} | roleId={}",
                employeeId,
                roleId
        );
    }

    public List<RoleDefinition> getAllRoles() {

        log.info("START getAllRoles");

        List<RoleDefinition> roles = roleRepo.findAll();

        log.info("END getAllRoles | count={}", roles.size());

        return roles;
    }

    public boolean hasRole(String employeeId, String requiredRoleId) {

        log.debug(
                "Checking role access | employeeId={} | requiredRoleId={}",
                employeeId,
                requiredRoleId
        );

        UserRoleMapping mapping1 =
                userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, requiredRoleId);

        UserRoleMapping mapping2 =
                userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, "System-Administrator");

        boolean hasAccess =
                (mapping2 != null && Boolean.TRUE.equals(mapping2.getAssignedRoleStatus()))
                        || (mapping1 != null && Boolean.TRUE.equals(mapping1.getAssignedRoleStatus()));

        log.debug(
                "Role access result | employeeId={} | requiredRoleId={} | hasAccess={}",
                employeeId,
                requiredRoleId,
                hasAccess
        );

        return hasAccess;
    }

}
