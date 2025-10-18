package com.ldap.myidcustomerservice.service;

import com.ldap.myidcustomerservice.dto.GroupsRequest;
import com.ldap.myidcustomerservice.dto.ManageGroupRequest;
import com.ldap.myidcustomerservice.dto.ModifyGroupResponse;
import com.ldap.myidcustomerservice.repository.GroupRepository;
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
