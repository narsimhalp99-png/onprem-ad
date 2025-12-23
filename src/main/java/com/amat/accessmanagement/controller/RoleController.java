package com.amat.accessmanagement.controller;

import com.amat.accessmanagement.entity.RoleDefinition;
import com.amat.accessmanagement.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/access-management/roles")
public class RoleController {

    @Autowired
    RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleDefinition>> getRoles() {

        log.info("API HIT: getRoles");

        List<RoleDefinition> roles = roleService.getAllRoles();

        log.info("Roles fetched successfully | count={}", roles.size());

        return ResponseEntity.ok(roles);
    }

}
