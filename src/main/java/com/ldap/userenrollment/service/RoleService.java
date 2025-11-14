package com.ldap.userenrollment.service;

import com.ldap.userenrollment.dto.AssignRoleRequest;
import com.ldap.userenrollment.entity.RoleDefinition;
import com.ldap.userenrollment.entity.UserEntity;
import com.ldap.userenrollment.entity.UserRoleMapping;
import com.ldap.userenrollment.exception.NotFoundException;
import com.ldap.userenrollment.repository.RoleRepository;
import com.ldap.userenrollment.repository.UserEnrollmentRepository;
import com.ldap.userenrollment.repository.UserRoleMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class RoleService {

    @Autowired
    UserEnrollmentRepository userRepo;

    @Autowired
    RoleRepository roleRepo;

    @Autowired
    UserRoleMappingRepository userRoleRepo;

    @Transactional
    public UserRoleMapping assignRole(Long employeeId, AssignRoleRequest req) {
        if (employeeId == null) throw new IllegalArgumentException("EmployeeId must not be null");
        if (req.getRoleId() == null || req.getRoleId().isBlank())
            throw new IllegalArgumentException("AssigningRoleId must not be null or empty");

        String roleId = req.getRoleId();
        Boolean isActive = req.getIsRoleActive() == null ? Boolean.TRUE : req.getIsRoleActive();

        UserRoleMapping existing = userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);

        if (existing != null) {
            // idempotent: update status if needed and return
            if (!Objects.equals(existing.getAssignedRoleStatus(), isActive)) {
                existing.setAssignedRoleStatus(isActive);
                // optionally update assignedAt to now if you want to track re-assign time
                existing.setAssignedAt(OffsetDateTime.now());
                return userRoleRepo.save(existing);
            }
            return existing;
        }

        // create new mapping
        UserRoleMapping mapping = new UserRoleMapping();
        mapping.setEmployeeId(employeeId);
        mapping.setAssignedRoleId(roleId);
        mapping.setAssignedRoleStatus(isActive);
        // assignedAt set in @PrePersist
        try {
            return userRoleRepo.save(mapping);
        } catch (DataIntegrityViolationException ex) {
            // This should be rare if constraint exists; handle gracefully
            // Either wrap and rethrow or fetch existing and return it (race condition)
            UserRoleMapping already = userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);
            if (already != null) return already;
            throw ex;
        }
    }


    @Transactional
    public void revokeRole(Long employeeId, String roleId) {
        UserRoleMapping existing =
                userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);

        if (existing == null) {
            throw new RuntimeException("Role not assigned to this user");
        }

        existing.setAssignedRoleStatus(false);
        userRoleRepo.save(existing);
    }




}
