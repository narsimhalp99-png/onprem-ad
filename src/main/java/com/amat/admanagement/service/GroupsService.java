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

        return  groupRepository.getGroupsPaged(groupsRequest);

    }


    public ModifyGroupResponse modifyGroup(ManageGroupRequest request){

        return groupRepository.modifyGroup(request);

    }
}
