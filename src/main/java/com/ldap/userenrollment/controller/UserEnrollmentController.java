package com.ldap.userenrollment.controller;

import com.ldap.userenrollment.dto.AssignRoleRequest;
import com.ldap.userenrollment.entity.UserEntity;
import com.ldap.userenrollment.entity.UserRoleMapping;
import com.ldap.userenrollment.service.RoleService;
import com.ldap.userenrollment.service.UserEnrollmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/myidcustomapi/access-mangement/users")
public class UserEnrollmentController {

    @Autowired
    UserEnrollmentService svc;


    @Autowired
    RoleService roleSvc;

    @PostMapping
    public ResponseEntity<UserEntity> createUser(@Valid @RequestBody UserEntity user) {
        UserEntity created = svc.createUser(user);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<Page<UserEntity>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(svc.getUsers(pageable));
    }

    @GetMapping("/{employeeId}/{additionalDetails}")
    public ResponseEntity<UserEntity> getUser(@PathVariable Long employeeId,@PathVariable boolean additionalDetails) {
        return ResponseEntity.ok(svc.getUser(employeeId,additionalDetails));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<UserEntity> updateUser(@PathVariable Long employeeId,
                                                 @RequestBody UserEntity user) {
        return ResponseEntity.ok(svc.updateUser(employeeId, user));
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

    @PatchMapping("/revoke-role/{employeeId}")
    public ResponseEntity<Void> revokeRole(@PathVariable Long employeeId, @RequestBody AssignRoleRequest req) {
        roleSvc.revokeRole(employeeId, req.getRoleId());
        return ResponseEntity.noContent().build();
    }
}
