package com.ldap.userenrollment.controller;

import com.ldap.userenrollment.dto.AssignRoleRequest;
import com.ldap.userenrollment.entity.UserRoleMapping;
import com.ldap.userenrollment.service.RoleService;
import com.ldap.userenrollment.service.UserEnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {


    @Autowired
    RoleService svc;



    @PostMapping("/assignRoleById/{employeeId}")
    public ResponseEntity<UserRoleMapping> assignRole(@PathVariable Long employeeId,
                                                      @RequestBody AssignRoleRequest req) {
        UserRoleMapping mapping = svc.assignRole(employeeId, req);
        return ResponseEntity.ok(mapping);
    }

    @PatchMapping("/revokeRoleById/{employeeId}")
    public ResponseEntity<Void> revokeRole(@PathVariable Long employeeId, @RequestBody AssignRoleRequest req) {
        svc.revokeRole(employeeId, req.getRoleId());
        return ResponseEntity.noContent().build();
    }
}
