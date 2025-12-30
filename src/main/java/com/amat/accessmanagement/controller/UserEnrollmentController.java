package com.amat.accessmanagement.controller;

import com.amat.accessmanagement.dto.UpdateRolesRequest;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.service.RoleService;
import com.amat.accessmanagement.service.UserEnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/access-management/users")
public class UserEnrollmentController {

    @Autowired
    UserEnrollmentService svc;

    @Autowired
    RoleService roleSvc;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserEntity user) {

        log.info("API HIT: createUser | employeeId={}", user.getEmployeeId());

        try {
            svc.createUser(user);

            log.info("User created successfully | employeeId={}", user.getEmployeeId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of("message", "User created successfully"));

        } catch (Exception ex) {

            log.error(
                    "Failed to create user | employeeId={} | error={}",
                    user.getEmployeeId(),
                    ex.getMessage(),
                    ex
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Failed to create user",
                            "error", ex.getMessage()
                    ));
        }
    }

    @GetMapping
    public ResponseEntity<Page<UserEntity>> getUsers(Pageable pageable) {

        log.info(
                "API HIT: getUsers | page={} | size={}",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<UserEntity> users = svc.getUsers(pageable);

        log.info(
                "Users fetched successfully | totalElements={}",
                users.getTotalElements()
        );

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<Object> getUser(
            @PathVariable String employeeId,
            @RequestParam(name = "additionalDetails", defaultValue = "false") boolean additionalDetails) {

        log.info(
                "API HIT: getUser | employeeId={} | additionalDetails={}",
                employeeId,
                additionalDetails
        );

        UserEntity user = svc.getUser(employeeId, additionalDetails);

        if (user == null) {

            log.warn("User not found | employeeId={}", employeeId);

            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", "failed",
                            "message", "User not found: " + employeeId
                    )
            );
        }

        log.info("User fetched successfully | employeeId={}", employeeId);

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "data", user
                )
        );
    }

    @PatchMapping("/{employeeId}")
    public ResponseEntity<?> updateUserInfo(
            @PathVariable String employeeId,
            @RequestBody UserEntity user) {

        log.info("API HIT: updateUserInfo | employeeId={}", employeeId);

        try {
            svc.updateUserDetails(employeeId, user);

            log.info("User updated successfully | employeeId={}", employeeId);

            return ResponseEntity.ok(
                    Map.of("message", "User updated successfully")
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to update user | employeeId={} | error={}",
                    employeeId,
                    ex.getMessage(),
                    ex
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Failed to update user",
                            "error", ex.getMessage()
                    ));
        }
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String employeeId) {

        log.info("API HIT: deleteUser | employeeId={}", employeeId);

        svc.deleteUser(employeeId);

        log.info("User deleted successfully | employeeId={}", employeeId);

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{employeeId}/roles")
    public ResponseEntity<Map<String, String>> updateUserRoles(
            @PathVariable String employeeId,
            @RequestBody UpdateRolesRequest req
    ) {

        log.info("Received role update request | employeeId={}", employeeId);

        roleSvc.updateRoles(employeeId, req);

        return ResponseEntity.ok(
                Map.of("message", "Roles updated successfully")
        );
    }

    @PostMapping("/encode")
    public String encodePassword(@RequestParam String password) {

        log.info("API HIT: encodePassword");

        log.debug(
                "Encoding password | length={}",
                password != null ? password.length() : 0
        );

        String encodedPassword = new BCryptPasswordEncoder().encode(password);

        log.info("Password encoded successfully");

        return encodedPassword;
    }
}
