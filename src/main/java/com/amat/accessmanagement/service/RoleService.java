package com.amat.accessmanagement.service;

import com.amat.accessmanagement.dto.UpdateRolesRequest;
import com.amat.accessmanagement.entity.RoleDefinition;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.entity.UserRoleMapping;
import com.amat.accessmanagement.repository.RoleRepository;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.accessmanagement.repository.UserRoleMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
public class RoleService {

    @Autowired
    UserEnrollmentRepository userRepo;

    @Autowired
    RoleRepository roleRepo;

    @Autowired
    UserRoleMappingRepository userRoleRepo;


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

    @Transactional
    public void updateRoles(
            String employeeId,
            UpdateRolesRequest req
    ) {

        log.info("START :: Role update | employeeId={} | operation={} | roles={}",
                employeeId, req.getOperation(), req.getRoles());

        UserEntity user = userRepo.findByEmployeeId(employeeId)
                .orElseThrow(() -> {
                    log.warn("User not found | employeeId={}", employeeId);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found"
                    );
                });

        if (req.getRoles() == null || req.getRoles().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one role must be provided"
            );
        }

        String op = req.getOperation().toUpperCase();

        for (String role : req.getRoles()) {

            if ("ADD".equals(op)) {

                if (userRoleRepo.existsByUserEmployeeIdAndAssignedRoleId(employeeId, role)) {
                    log.warn("Role already assigned | employeeId={} | role={}",
                            employeeId, role);
//                    throw new ResponseStatusException(
//                            HttpStatus.CONFLICT,
//                            "Role already assigned: " + role
//                    );
                    continue;
                }

                UserRoleMapping mapping = UserRoleMapping.builder()
                        .user(user)
                        .assignedRoleId(role)
                        .assignedRoleStatus(true)
                        .build();

                userRoleRepo.save(mapping);
                log.info("Role added | employeeId={} | role={}", employeeId, role);

            } else if ("REMOVE".equals(op)) {

                userRoleRepo.deleteByUserEmployeeIdAndAssignedRoleId(employeeId, role);
                log.info("Role removed | employeeId={} | role={}", employeeId, role);

            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid operation. Use ADD or REMOVE"
                );
            }
        }

        log.info("END :: Role update | employeeId={}", employeeId);
    }




}
