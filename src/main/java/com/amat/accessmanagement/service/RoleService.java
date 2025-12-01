package com.amat.accessmanagement.service;

import com.amat.accessmanagement.dto.AssignRoleRequest;
import com.amat.accessmanagement.entity.RoleDefinition;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.entity.UserRoleMapping;
import com.amat.accessmanagement.repository.RoleRepository;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.accessmanagement.repository.UserRoleMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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
        if (employeeId == null)
            throw new IllegalArgumentException("EmployeeId must not be null");

        if (req.getRoleId() == null || req.getRoleId().isBlank())
            throw new IllegalArgumentException("AssigningRoleId must not be null or empty");

        String roleId = req.getRoleId();
        Boolean isActive = req.getIsRoleActive() == null ? Boolean.TRUE : req.getIsRoleActive();


        UserEntity userEntity = userRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("User not found: " + employeeId));

        UserRoleMapping existing = userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);

        if (existing != null) {
            // Idempotent update
            if (!Objects.equals(existing.getAssignedRoleStatus(), isActive)) {
                existing.setAssignedRoleStatus(isActive);
                return userRoleRepo.save(existing);
            }
            return existing;
        }

        UserRoleMapping mapping = new UserRoleMapping();
        mapping.setUser(userEntity);           // FK
        mapping.setAssignedRoleId(roleId);
        mapping.setAssignedRoleStatus(isActive);

        try {
            return userRoleRepo.save(mapping);
        } catch (DataIntegrityViolationException ex) {

            // If unique constraint violated due to race condition, return existing
            UserRoleMapping already = userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);
            if (already != null) return already;

            throw ex;
        }
    }


    @Transactional
    public void revokeRole(String employeeId, String roleId) {
        UserRoleMapping existing =
                userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, roleId);

        if (existing == null) {
            throw new RuntimeException("Role not assigned to this user");
        }

        existing.setAssignedRoleStatus(false);
        userRoleRepo.save(existing);
    }

    public List<RoleDefinition> getAllRoles() {
        return roleRepo.findAll();
    }

    public boolean hasRole(String employeeId, String requiredRoleId) {
        UserRoleMapping mapping1 =   userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, requiredRoleId);
        UserRoleMapping mapping2 =   userRoleRepo.findByEmployeeIdAndAssignedRoleId(employeeId, "System-Administrator");

        return (mapping2 != null && Boolean.TRUE.equals(mapping2.getAssignedRoleStatus()) )|| (mapping1 != null && Boolean.TRUE.equals(mapping1.getAssignedRoleStatus()));
    }



}
