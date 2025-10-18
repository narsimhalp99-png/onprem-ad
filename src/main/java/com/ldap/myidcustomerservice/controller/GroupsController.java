package com.ldap.myidcustomerservice.controller;

import com.ldap.myidcustomerservice.dto.GroupsRequest;
import com.ldap.myidcustomerservice.dto.ManageGroupRequest;
import com.ldap.myidcustomerservice.dto.ModifyGroupResponse;
import com.ldap.myidcustomerservice.service.GroupsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/activeDirectory/groups")
public class GroupsController {

    @Autowired
    GroupsService service;

    @PostMapping("/getGroups")
    public Map<String, Object> getGroups(@RequestBody GroupsRequest groupsRequest) {
        return service.fetchAllGroups(groupsRequest);
    }

    @PostMapping("/manageGroup")
    public ResponseEntity<ModifyGroupResponse> modifyGroup(@RequestBody ManageGroupRequest request)  {
        return ResponseEntity.ok(service.modifyGroup(request));
    }
}

