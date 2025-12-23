package com.amat.admanagement.service;

import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.ManageGroupRequest;
import com.amat.admanagement.dto.ModifyGroupResponse;
import com.amat.admanagement.repository.GroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class GroupsService {

    @Autowired
    GroupRepository groupRepository;

    public Map<String, Object> fetchAllGroups(GroupsRequest groupsRequest) {

        log.info("START fetchAllGroups");

        log.debug(
                "GroupsRequest | filter={} | pageNumber={} | pageSize={}",
                groupsRequest.getFilter(),
                groupsRequest.getPageNumber(),
                groupsRequest.getPageSize()
        );

        Map<String, Object> response = groupRepository.getGroupsPaged(groupsRequest);

        log.info(
                "END fetchAllGroups | responseKeys={}",
                response != null ? response.keySet() : "null"
        );

        return response;
    }

    public ModifyGroupResponse modifyGroupMembers(ManageGroupRequest request) {

        log.info(
                "START modifyGroupMembers | operation={} | groupDn={}",
                request.getOperation(),
                request.getGroupDn()
        );

        ModifyGroupResponse response = groupRepository.modifyGroupMembers(request);

        log.info(
                "END modifyGroupMembers | statusCode={}",
                response.getStatusCode()
        );

        return response;
    }
}
