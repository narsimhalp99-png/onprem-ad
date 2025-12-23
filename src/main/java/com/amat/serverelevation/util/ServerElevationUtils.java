package com.amat.serverelevation.util;

import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.ComputerService;
import com.amat.admanagement.service.GroupsService;
import com.amat.admanagement.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ServerElevationUtils {

    @Autowired
    GroupsService groupsService;

    @Autowired
    UserService userService;

    @Value("${spring.ldap.base:''}")
    String defaultBase;

    public String fetchUserDn(String employeeId) {

        log.info("START fetchUserDn | employeeId={}", employeeId);

        UsersRequest requestorReq = UsersRequest.builder()
                .filter("(employeeId=" + employeeId + ")")
                .searchBaseOU(defaultBase)
                .pageNumber(0)
                .pageSize(1)
                .build();

        log.debug("LDAP query constructed for user | employeeId={}", employeeId);

        Map<String, Object> requestorResp = userService.fetchAllObjects(requestorReq);
        List<Map<String, Object>> requestorData =
                (List<Map<String, Object>>) requestorResp.get("data");

        if (requestorData == null || requestorData.isEmpty()) {
            log.warn("User DN not found in LDAP | employeeId={}", employeeId);
            return "-1";
        }

        String userDn = (String) requestorData.get(0).get("distinguishedName");

        log.info("END fetchUserDn | employeeId={} | userDn={}", employeeId, userDn);

        return userDn;
    }

    public String fetchGroupDn(String groupName) {

        log.info("START fetchGroupDn | groupName={}", groupName);

        GroupsRequest requestorReq = GroupsRequest.builder()
                .filter("(cn=" + groupName + ")")
                .searchBaseOU(defaultBase)
                .pageNumber(0)
                .pageSize(1)
                .build();

        log.debug("LDAP query constructed for group | groupName={}", groupName);

        Map<String, Object> requestorResp = groupsService.fetchAllGroups(requestorReq);
        List<Map<String, Object>> requestorData =
                (List<Map<String, Object>>) requestorResp.get("data");

        if (requestorData == null || requestorData.isEmpty()) {
            log.warn("Group DN not found in LDAP | groupName={}", groupName);
            return "-1";
        }

        String groupDn = (String) requestorData.get(0).get("distinguishedName");

        log.info("END fetchGroupDn | groupName={} | groupDn={}", groupName, groupDn);

        return groupDn;
    }

    public String fetchAdminAccountDn(String userDn) {

        log.info("START fetchAdminAccountDn | userDn={}", userDn);

        UsersRequest adminReq = UsersRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(&(manager=" + userDn + ")(employeeType=SA)(!(userAccountControl=514)))")
                .pageNumber(0)
                .pageSize(5)
                .addtnlAttributes(List.of("manager", "employeeType", "userAccountControl"))
                .build();

        log.debug("LDAP query constructed for admin account | managerDn={}", userDn);

        Map<String, Object> ldapResponse = userService.fetchAllObjects(adminReq);
        List<Map<String, Object>> dataEntries =
                (List<Map<String, Object>>) ldapResponse.get("data");

        if (dataEntries == null || dataEntries.isEmpty()) {
            log.warn("Admin account not found | managerDn={}", userDn);
            return "";
        }

        String adminDn = (String) dataEntries.get(0).get("distinguishedName");

        String resolvedAdminDn = (adminDn != null && !adminDn.isBlank()) ? adminDn : "";

        log.info("END fetchAdminAccountDn | managerDn={} | adminDn={}", userDn, resolvedAdminDn);

        return resolvedAdminDn;
    }

}
