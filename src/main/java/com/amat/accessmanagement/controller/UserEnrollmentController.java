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
    public ResponseEntity<Object> getUser(
            @PathVariable String employeeId,
            @RequestParam(name = "additionalDetails", defaultValue = "false") boolean additionalDetails) {

        UserEntity user = svc.getUser(employeeId, additionalDetails);

        if (user == null) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", "failed",
                            "message", "User not found: " + employeeId
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "data", user
                )
        );
    }


//    @PutMapping("/{employeeId}")
//    public ResponseEntity<UserEntity> updateUser(@PathVariable String employeeId,
//                                                 @RequestBody UserEntity user) {
//        return ResponseEntity.ok(svc.updateUser(employeeId, user));
//    }

    @PatchMapping("/{employeeId}")
    public ResponseEntity<?> updateUserInfo(
            @PathVariable String employeeId,
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
    public ResponseEntity<Void> deleteUser(@PathVariable String employeeId) {
        svc.deleteUser(employeeId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{employeeId}/roles/assign")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable String employeeId,
            @RequestBody AssignRoleRequest req) {

        UserRoleMapping mapping = roleSvc.assignRole(employeeId, req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Role assigned successfully"
                ));
    }


    @PatchMapping("/{employeeId}/roles/remove")
    public ResponseEntity<Void> revokeRole(@PathVariable String employeeId, @RequestBody AssignRoleRequest req) {
        roleSvc.revokeRole(employeeId, req.getRoleId());
        return ResponseEntity.noContent().build();
    }
}
