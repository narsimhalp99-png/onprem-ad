package com.ldap.userenrollment.service;

import com.ldap.userenrollment.dto.AssignRoleRequest;
import com.ldap.userenrollment.entity.UserEntity;
import com.ldap.userenrollment.entity.UserRoleMapping;
import com.ldap.userenrollment.exception.NotFoundException;
import com.ldap.userenrollment.repository.RoleRepository;
import com.ldap.userenrollment.repository.UserEnrollmentRepository;
import com.ldap.userenrollment.repository.UserRoleRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Data
@Service
public class UserEnrollmentService {

    @Autowired
    UserEnrollmentRepository userRepo;

    @Autowired
    RoleRepository roleRepo;

    @Autowired
    UserRoleRepository userRoleRepo;



    public UserEntity createUser(UserEntity user) {
        return userRepo.save(user);
    }

    public Page<UserEntity> getUsers(Pageable pageable) {
        return userRepo.findAll(pageable);
    }

    public UserEntity getUser(Long employeeId) {
        return userRepo.findById(employeeId)
            .orElseThrow(() -> new NotFoundException("User not found: " + employeeId));
    }

    public UserEntity updateUser(Long employeeId, UserEntity update) {
        UserEntity existing = getUser(employeeId);
        if (update.getDisplayName() != null) existing.setDisplayName(update.getDisplayName());
        existing.setFirstName(update.getFirstName());
        existing.setLastName(update.getLastName());
        existing.setOrganization(update.getOrganization());
        existing.setSubOrganization(update.getSubOrganization());
        existing.setTitle(update.getTitle());
        existing.setManagerEmpId(update.getManagerEmpId());
        existing.setIsActive(update.getIsActive());
        return userRepo.save(existing);
    }

    public void deleteUser(Long employeeId) {
        UserEntity existing = getUser(employeeId);
        userRepo.delete(existing);
    }

    @Transactional
    public UserRoleMapping assignRole(Long employeeId, AssignRoleRequest req) {
        if (employeeId == null) {
            throw new IllegalArgumentException("EmployeeId must not be null");
        }

        if (req.getRoleName() == null || req.getRoleName().isBlank()) {
            throw new IllegalArgumentException("AssignedRoleName must not be null or empty");
        }

        UserRoleMapping mapping = new UserRoleMapping();
        mapping.setEmployeeId(employeeId);
        mapping.setAssignedRoleId(req.getRoleName());
        mapping.setAssignedRoleStatus(req.getIsRoleActive() == null || req.getIsRoleActive());

        // assignedAt will be automatically set by @PrePersist
        return userRoleRepo.save(mapping);
    }

    @Transactional
    public void revokeRole(Long employeeId, Long roleId) {
//        UserRoleId id = new UserRoleId(employeeId, roleId);
        UserRoleMapping existing = userRoleRepo.findById(roleId)
            .orElseThrow(() -> new NotFoundException("UserRole not found"));
        userRoleRepo.delete(existing);
    }
}
