package com.amat.usermanagement.controller;

import com.amat.usermanagement.dto.AssignRoleRequest;
import com.amat.usermanagement.entity.UserEntity;
import com.amat.usermanagement.entity.UserRoleMapping;
import com.amat.usermanagement.service.RoleService;
import com.amat.usermanagement.service.UserEnrollmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/myidcustomapi/access-management/users")
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
