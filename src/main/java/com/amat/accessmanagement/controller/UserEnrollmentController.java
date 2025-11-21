package com.amat.accessmanagement.controller;

import com.amat.accessmanagement.dto.AssignRoleRequest;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.entity.UserRoleMapping;
import com.amat.accessmanagement.service.RoleService;
import com.amat.accessmanagement.service.UserEnrollmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/access-management/users")
public class UserEnrollmentController {

    @Autowired
    UserEnrollmentService svc;


    @Autowired
    RoleService roleSvc;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserEntity user) {
        try {
            svc.createUser(user);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of("message", "User created successfully"));
        } catch (Exception ex) {
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
        return ResponseEntity.ok(svc.getUsers(pageable));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<UserEntity> getUser(@PathVariable Long employeeId, @RequestParam(name = "additionalDetails", defaultValue = "false") boolean additionalDetails) {
        return ResponseEntity.ok(svc.getUser(employeeId,additionalDetails));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<UserEntity> updateUser(@PathVariable Long employeeId,
                                                 @RequestBody UserEntity user) {
        return ResponseEntity.ok(svc.updateUser(employeeId, user));
    }

    @PatchMapping("/{employeeId}")
    public ResponseEntity<?> updateUserInfo(
            @PathVariable Long employeeId,
            @RequestBody UserEntity user) {

        try {
            svc.updateUserDetails(employeeId, user);
            return ResponseEntity.ok(
                    Map.of("message", "User updated successfully")
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Failed to update user",
                            "error", ex.getMessage()
                    ));
        }
    }



    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long employeeId) {
        svc.deleteUser(employeeId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/assign-role/{employeeId}")
    public ResponseEntity<UserRoleMapping> assignRole(@PathVariable Long employeeId,
                                                      @RequestBody AssignRoleRequest req) {
        UserRoleMapping mapping = roleSvc.assignRole(employeeId, req);
        return ResponseEntity.ok(mapping);
    }

    @PatchMapping("/remove-role/{employeeId}")
    public ResponseEntity<Void> revokeRole(@PathVariable Long employeeId, @RequestBody AssignRoleRequest req) {
        roleSvc.revokeRole(employeeId, req.getRoleId());
        return ResponseEntity.noContent().build();
    }
}
