package com.amat.admanagement.controller;

import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.ManageGroupRequest;
import com.amat.admanagement.dto.ModifyGroupResponse;
import com.amat.admanagement.service.GroupsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ad-management/groups")
public class GroupsController {

    @Autowired
    GroupsService service;

    @PostMapping
    public Map<String, Object> getGroups(@RequestBody GroupsRequest groupsRequest) {

        log.info("API HIT: getGroups");

        log.debug(
                "GroupsRequest received | filter={} | pageNumber={} | pageSize={}",
                groupsRequest.getFilter(),
                groupsRequest.getPageNumber(),
                groupsRequest.getPageSize()
        );

        Map<String, Object> response = service.fetchAllGroups(groupsRequest);

        log.info(
                "Groups fetched successfully | resultKeys={}",
                response != null ? response.keySet() : "null"
        );

        return response;
    }

    @PostMapping("/manage-members")
    public ResponseEntity<ModifyGroupResponse> modifyGroupMembers(
            @RequestBody ManageGroupRequest request)  {

        log.info(
                "API HIT: modifyGroupMembers | operation={} | groupDn={}",
                request.getOperation(),
                request.getGroupDn()
        );

        ModifyGroupResponse response = service.modifyGroupMembers(request);

        log.info(
                "Group member modification completed | statusCode={}",
                response.getStatusCode()
        );

        return ResponseEntity.ok(response);
    }
}
