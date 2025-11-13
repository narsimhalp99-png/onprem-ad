package com.ldap.userenrollment.controller;

import com.ldap.userenrollment.dto.AssignRoleRequest;
import com.ldap.userenrollment.entity.UserEntity;
import com.ldap.userenrollment.entity.UserRoleMapping;
import com.ldap.userenrollment.service.UserEnrollmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserEnrollmentController {

    @Autowired
    UserEnrollmentService svc;

    @PostMapping("/createUser")
    public ResponseEntity<UserEntity> createUser(@Valid @RequestBody UserEntity user) {
        UserEntity created = svc.createUser(user);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/getUsers")
    public ResponseEntity<Page<UserEntity>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(svc.getUsers(pageable));
    }

    @GetMapping("/getUserById/{employeeId}")
    public ResponseEntity<UserEntity> getUser(@PathVariable Long employeeId) {
        return ResponseEntity.ok(svc.getUser(employeeId));
    }

    @PutMapping("/updateUserById/{employeeId}")
    public ResponseEntity<UserEntity> updateUser(@PathVariable Long employeeId,
                                                 @RequestBody UserEntity user) {
        return ResponseEntity.ok(svc.updateUser(employeeId, user));
    }

    @DeleteMapping("/deleteUserById/{employeeId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long employeeId) {
        svc.deleteUser(employeeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assignRoleById/{employeeId}/roles")
    public ResponseEntity<UserRoleMapping> assignRole(@PathVariable Long employeeId,
                                                      @RequestBody AssignRoleRequest req) {
        UserRoleMapping mapping = svc.assignRole(employeeId, req);
        return ResponseEntity.ok(mapping);
    }

    @DeleteMapping("/revokeRoleById/{employeeId}/roles/{roleId}")
    public ResponseEntity<Void> revokeRole(@PathVariable Long employeeId, @PathVariable Long roleId) {
        svc.revokeRole(employeeId, roleId);
        return ResponseEntity.noContent().build();
    }
}
