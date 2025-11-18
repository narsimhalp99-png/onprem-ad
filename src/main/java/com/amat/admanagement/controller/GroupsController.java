package com.amat.admanagement.controller;

import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.ManageGroupRequest;
import com.amat.admanagement.dto.ModifyGroupResponse;
import com.amat.admanagement.service.GroupsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/myidcustomapi/ad-management/groups")
public class GroupsController {

    @Autowired
    GroupsService service;

    @PostMapping
    public Map<String, Object> getGroups(@RequestBody GroupsRequest groupsRequest) {
        return service.fetchAllGroups(groupsRequest);
    }

    @PostMapping("/manage-members")
    public ResponseEntity<ModifyGroupResponse> modifyGroup(@RequestBody ManageGroupRequest request)  {
        return ResponseEntity.ok(service.modifyGroup(request));
    }
}

