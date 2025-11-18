package com.amat.serverelevation.service;


import com.amat.admanagement.dto.ComputersRequest;
import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.ComputerService;
import com.amat.admanagement.service.GroupsService;
import com.amat.admanagement.service.UserService;
import com.amat.serverelevation.DTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
public class ServerElevationService {


    @Autowired
    ComputerService computerService;

    @Autowired
    GroupsService groupsService;

    @Autowired
    UserService userService;

    @Autowired
    LdapService ldapService;

    @Value("${spring.ldap.base:''}")
    String defaultBase;


    public ServerElevationResponse validateRequest(ServerElevationRequest request, boolean additionalDetails) {
        ServerElevationResponse response = ServerElevationResponse.builder()
                .computerName(request.getComputerName())
                .build();

        // Step 0: Verify Requestor Exists in AD
        UsersRequest requestorReq = UsersRequest.builder()
                .filter("(employeeId=" + request.getRequestorEmpId() + ")")
                .searchBaseOU(defaultBase)
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> requestorResp = userService.fetchAllObjects(requestorReq);
        List<Map<String, Object>> requestorData = (List<Map<String, Object>>) requestorResp.get("data");

        if (requestorData == null || requestorData.isEmpty()) {
            setError(response, "USER_NOT_FOUND");
            return response;
        }

        String requestorDn = (String) requestorData.get(0).get("distinguishedName");

        // Step 1: Find Active AD Computer
        ComputersRequest computerReq = ComputersRequest.builder()
                .filter("(&(cn=" + request.getComputerName() + ")(!(userAccountControl=4130)))")
                .searchBaseOU(defaultBase)
                .pageNumber(0)
                .pageSize(1)
                .addtnlAttributes(List.of("extensionAttribute1", "userAccountControl", "operatingSystem"))
                .build();

        Map<String, Object> computerResult = computerService.fetchAllObjects(computerReq);
        List<Map<String, Object>> computers = (List<Map<String, Object>>) computerResult.get("data");

        if (computers == null || computers.isEmpty()) {
            setError(response, "SERVER_NOT_FOUND");
            return response;
        }

        Map<String, Object> computer = computers.get(0);
        response.setOperatingSystem((String) computer.get("operatingSystem"));
        response.setApplicationName("MyID-IIQ"); // Or derive if needed

        // Step 2 & 3: Find Admin Group
        String adminGroup = request.getComputerName() + "-APP-ADMINS";
        GroupsRequest groupReq = GroupsRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(cn=" + adminGroup + ")")
                .addtnlAttributes(List.of("managedBy"))
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> groupResult = groupsService.fetchAllGroups(groupReq);
        List<Map<String, Object>> groups = (List<Map<String, Object>>) groupResult.get("data");

        if (groups == null || groups.isEmpty()) {
            setError(response, "SERVER_OWNER_NOT_FOUND");
            return response;
        }

        String ownerDn = (String) groups.get(0).get("managedBy");

        // Step 4: Look up Owner Info
        UsersRequest userReq = UsersRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(distinguishedName=" + ownerDn + ")")
                .addtnlAttributes(List.of("employeeId", "displayName"))
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> userResult = userService.fetchAllObjects(userReq);
        List<Map<String, Object>> users = (List<Map<String, Object>>) userResult.get("data");

        if (users == null || users.isEmpty()) {
            setError(response, "OWNER_NOT_FOUND");
            return response;
        }

        Map<String, Object> owner = users.get(0);

        response.setOwnerDetails(OwnerDetails.builder()
                .ownerEmpID((String) owner.get("employeeId"))
                .ownerName((String) owner.get("displayName"))
                .build());

        // Step 5: Check Local Admin Membership
        String localAdminGroup = request.getComputerName() + "-Local-Admins";
        GroupsRequest localGroupReq = GroupsRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(cn=" + localAdminGroup + ")")
                .addtnlAttributes(List.of("member"))
                .pageNumber(0)
                .pageSize(1)
                .build();

        Map<String, Object> localGroupResult = groupsService.fetchAllGroups(localGroupReq);
        List<Map<String, Object>> localGroups = (List<Map<String, Object>>) localGroupResult.get("data");

        if (localGroups == null || localGroups.isEmpty()) {
            setError(response, "ELEVATION_GROUP_NOT_FOUND");
            return response;
        }

        List<String> members = (List<String>) localGroups.get(0).get("member");
        if (members != null && members.contains(request.getRequestorEmpId())) {
            setError(response, "USER_ALREADY_ELEVATED");
            return response;
        }

        response.setEligibleForElevation(true);

        // Step 6: Need Approval?
        if (additionalDetails) {
            GroupsRequest recursiveGroupReq = GroupsRequest.builder()
                    .searchBaseOU(defaultBase)
                    .filter("(cn=" + adminGroup + ")")
                    .pageSize(2)
                    .pageNumber(0)
                    .fetchRecursiveMembers(true)
                    .build();

            Map<String, Object> recursiveGroupResult = groupsService.fetchAllGroups(recursiveGroupReq);

            List<Map<String, Object>> recursiveGroupResultData = (List<Map<String, Object>>) recursiveGroupResult.get("data");

            List<String> recursiveMembers = (List<String>) recursiveGroupResultData.get(0).get("member");

            String userAdminDn = ldapService.fetchAdminAccountDn(requestorDn);

            boolean isMemberOfAdminGroup = recursiveMembers.contains(userAdminDn);
            response.setApprovalRequired(!isMemberOfAdminGroup);
        }

        return response;
    }

    private void setError(ServerElevationResponse response, String errorMsg) {
        response.setEligibleForElevation(false);
        response.setEligibleForElevationMsg(errorMsg);
    }
}
