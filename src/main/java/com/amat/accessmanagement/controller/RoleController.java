package com.amat.accessmanagement.controller;

import com.amat.accessmanagement.entity.RoleDefinition;
import com.amat.accessmanagement.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/access-management/roles")
public class RoleController {


    @Autowired
    RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleDefinition>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }


}
