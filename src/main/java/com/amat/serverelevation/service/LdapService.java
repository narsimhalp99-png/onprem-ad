package com.amat.serverelevation.service;


import com.amat.admanagement.service.ComputerService;
import com.amat.admanagement.service.GroupsService;
import com.amat.admanagement.service.UserService;
import com.amat.admanagement.dto.ComputersRequest;
import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.UsersRequest;
import com.amat.serverelevation.DTO.AdComputer;
import com.amat.serverelevation.DTO.AdGroup;
import com.amat.serverelevation.DTO.AdUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@Slf4j
public class LdapService {

    @Autowired
    ComputerService computerService;

    @Autowired
    GroupsService groupsService;

    @Autowired
    UserService userService;

    @Value("${spring.ldap.base:''}")
    String defaultBase;

    // Fetch an Active Directory Computer object by CN
    public Optional<AdComputer> findActiveComputer(String serverName) {
        ComputersRequest req = ComputersRequest.builder()
                .filter("(cn=" + serverName + ")")
                .searchBaseOU(defaultBase)
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> result = computerService.fetchAllObjects(req);
        List<Map<String, Object>> computers = (List<Map<String, Object>>) result.get("data");

        if (computers != null && !computers.isEmpty()) {
            Map<String, Object> comp = computers.get(0);
            return Optional.of(AdComputer.builder()
                    .name(serverName)
                    .operatingSystem((String) comp.get("operatingSystem"))
                    .build());
        }
        return Optional.empty();
    }

    // Fetch AD Group by Name and extract 'managedBy' attribute
    public Optional<AdGroup> findGroupWithManagedBy(String groupName) {
        GroupsRequest req = GroupsRequest.builder()
                .filter("(cn=" + groupName + ")")
                .searchBaseOU(defaultBase)
                .addtnlAttributes(List.of("managedBy"))
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> result = groupsService.fetchAllGroups(req);
        List<Map<String, Object>> groups = (List<Map<String, Object>>) result.get("data");

        if (groups != null && !groups.isEmpty()) {
            Map<String, Object> group = groups.get(0);
            return Optional.of(AdGroup.builder()
                    .name(groupName)
                    .managedBy((String) group.get("managedBy"))
                    .build());
        }
        return Optional.empty();
    }

    // Fetch User by Distinguished Name (DN)
    public Optional<AdUser> getUserByDn(String dn) {
        UsersRequest req = UsersRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(distinguishedName=" + dn + ")")
                .addtnlAttributes(List.of("employeeId", "displayName"))
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> result = userService.fetchAllObjects(req);
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("data");

        if (users != null && !users.isEmpty()) {
            Map<String, Object> user = users.get(0);
            return Optional.of(AdUser.builder()
                    .dn(dn)
                    .employeeId((String) user.get("employeeId"))
                    .displayName((String) user.get("displayName"))
                    .build());
        }
        return Optional.empty();
    }

    // Check if a user is a member of a specific AD group
    public boolean isUserInGroup(String employeeId, String groupName) {
        GroupsRequest req = GroupsRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(cn=" + groupName + ")")
                .addtnlAttributes(List.of("member"))
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> result = groupsService.fetchAllGroups(req);
        List<Map<String, Object>> groups = (List<Map<String, Object>>) result.get("data");

        if (groups != null && !groups.isEmpty()) {
            List<String> members = (List<String>) groups.get(0).get("member");
            if (members != null) {
                return members.stream().anyMatch(memberDn -> memberDn.contains(employeeId));
            }
        }
        return false;
    }


    public String fetchAdminAccountDn(String userDn) {
        UsersRequest adminReq = UsersRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(&(manager=" + userDn + ")(employeeType=SA)(!(userAccountControl=514)))")
                .pageNumber(0)
                .pageSize(5)
                .addtnlAttributes(List.of("manager", "employeeType", "userAccountControl"))
                .build();

        Map<String, Object> ldapResponse = userService.fetchAllObjects(adminReq);
        List<Map<String, Object>> dataEntries = (List<Map<String, Object>>) ldapResponse.get("data");

        if (dataEntries == null || dataEntries.isEmpty()) {
            return "";
        }

        String adminDn = (String) dataEntries.get(0).get("distinguishedName");

        return (adminDn != null && !adminDn.isBlank()) ? adminDn : "";
    }

}
