package com.ldap.userenrollment.service;

import com.ldap.userenrollment.dto.AssignRoleRequest;
import com.ldap.userenrollment.entity.UserEntity;
import com.ldap.userenrollment.entity.UserRoleMapping;
import com.ldap.userenrollment.exception.NotFoundException;
import com.ldap.userenrollment.repository.RoleRepository;
import com.ldap.userenrollment.repository.UserEnrollmentRepository;
import com.ldap.userenrollment.repository.UserRoleMappingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

@Data
@Service
public class UserEnrollmentService {

    @Autowired
    UserEnrollmentRepository userRepo;

    @Autowired
    RoleRepository roleRepo;

    @Autowired
    UserRoleMappingRepository userRoleRepo;



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



}
