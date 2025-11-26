package com.amat.serverelevation.util;


import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.ComputerService;
import com.amat.admanagement.service.GroupsService;
import com.amat.admanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ServerElevationUtils {


    @Autowired
    GroupsService groupsService;

    @Autowired
    UserService userService;


    @Value("${spring.ldap.base:''}")
    String defaultBase;

    public String fetchUserDn(String employeeId) {

        UsersRequest requestorReq = UsersRequest.builder()
                .filter("(employeeId=" + employeeId + ")")
                .searchBaseOU(defaultBase)
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> requestorResp = userService.fetchAllObjects(requestorReq);
        List<Map<String, Object>> requestorData = (List<Map<String, Object>>) requestorResp.get("data");

        if (requestorData == null || requestorData.isEmpty()) {
            return "-1";
        }

        return (String) requestorData.get(0).get("distinguishedName");

    }

    public String fetchGroupDn(String groupName) {

        GroupsRequest requestorReq = GroupsRequest.builder()
                .filter("(cn=" + groupName + ")")
                .searchBaseOU(defaultBase)
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> requestorResp = groupsService.fetchAllGroups(requestorReq);
        List<Map<String, Object>> requestorData = (List<Map<String, Object>>) requestorResp.get("data");

        if (requestorData == null || requestorData.isEmpty()) {
            return "-1";
        }

        return (String) requestorData.get(0).get("distinguishedName");

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
