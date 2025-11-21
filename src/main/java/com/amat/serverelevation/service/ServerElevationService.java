package com.amat.serverelevation.service;


import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.exception.NotFoundException;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.admanagement.dto.ComputersRequest;
import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.ComputerService;
import com.amat.admanagement.service.GroupsService;
import com.amat.admanagement.service.UserService;
import com.amat.serverelevation.DTO.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Slf4j
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


    @Autowired
    UserEnrollmentRepository userRepo;

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

        String userAdminDn = ldapService.fetchAdminAccountDn(requestorDn);


        if (userAdminDn == null || userAdminDn.isEmpty()) {
            setError(response, "USER_ADMIN_ACCOUNT_NOT_FOUND");
            return response;
        }


        // Step 1: Find Active AD Computer
        ComputersRequest computerReq = ComputersRequest.builder()
                .filter("(&(cn=" + request.getComputerName() + ")(!(userAccountControl=4130)))")
                .searchBaseOU(defaultBase)
                .pageNumber(0)
                .pageSize(1)
                .addtnlAttributes(List.of("facsimileTelephoneNumber", "userAccountControl", "operatingSystem"))
                .build();

        Map<String, Object> computerResult = computerService.fetchAllObjects(computerReq);
        List<Map<String, Object>> computers = (List<Map<String, Object>>) computerResult.get("data");

        if (computers == null || computers.isEmpty()) {
            setError(response, "SERVER_NOT_FOUND");
            return response;
        }

        Map<String, Object> computer = computers.get(0);
        response.setOperatingSystem((String) computer.get("operatingSystem"));
        response.setApplicationName((String) computer.get("facsimileTelephoneNumber")); // Or derive if needed

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

        // TODO Single item, string , multiple -> List coming
//        List<String> members = (List<String>) localGroups.get(0).get("member");

        Object memberObj = localGroups.get(0).get("member");

        List<String> members =
                memberObj instanceof List
                        ? (List<String>) memberObj
                        : Collections.singletonList(String.valueOf(memberObj));


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

            // Collect all members from all results
            List<String> allRecursiveMembers = new ArrayList<>();

            for (Map<String, Object> groupData : recursiveGroupResultData) {
                Object memberObj2 = groupData.get("member");

                if (memberObj2 instanceof List) {
                    allRecursiveMembers.addAll((List<String>) memberObj2);

                } else if (memberObj2 instanceof String) {
                    allRecursiveMembers.add((String) memberObj2);

                } else if (memberObj2 != null) {
                    allRecursiveMembers.add(memberObj2.toString());
                }
            }


            boolean isMemberOfAdminGroup = allRecursiveMembers.contains(userAdminDn);
            response.setApprovalRequired(!isMemberOfAdminGroup);
        }

        return response;
    }

    private void setError(ServerElevationResponse response, String errorMsg) {
        response.setEligibleForElevation(false);
        response.setEligibleForElevationMsg(errorMsg);
    }

    public Map<String, Object> fetchAdminAccountDetails(Long employeeId) {

        Map<String, Object> adminDetails = null;
        // Fetch user from DB
        UserEntity user = userRepo.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("User not found: " + employeeId));


            try {
                UsersRequest firstReq = new UsersRequest();
                firstReq.setSearchBaseOU(defaultBase);
                firstReq.setFilter("employeeId=" + employeeId);
                firstReq.setPageNumber(0);
                firstReq.setPageSize(5);
                firstReq.setAddtnlAttributes(
                        List.of("manager", "employeeType", "userAccountControl")
                );

                Map<String, Object> firstResp = userService.fetchAllObjects(firstReq);

                // Extract "distinguishedName" from first response
                List<Map<String, Object>> dataList =
                        (List<Map<String, Object>>) firstResp.get("data");

                String distinguishedName = (String) dataList.get(0).get("distinguishedName");

                if (!dataList.isEmpty() && distinguishedName!=null){
                    user.setRegularAccountDN(distinguishedName);
                }

                if (dataList == null || dataList.isEmpty()) {
                    throw new NotFoundException("LDAP user not found for employeeId: " + employeeId);
                }

                UsersRequest secondReq = new UsersRequest();
                secondReq.setSearchBaseOU(distinguishedName);
                secondReq.setFilter("(&(manager=" + distinguishedName + ")(employeeType=SA)(!(userAccountControl=514)))");
                secondReq.setPageNumber(0);
                secondReq.setPageSize(5);
                secondReq.setAddtnlAttributes(
                        List.of("manager", "employeeType", "userAccountControl")
                );

                Map<String, Object> finalLdapResp = userService.fetchAllObjects(secondReq);


                List<Map<String, Object>> newDataList = (List<Map<String, Object>>) finalLdapResp.get("data");

                adminDetails = newDataList.get(0);


            }catch(Exception e){
                log.info("fetchAdminAccountDetails ::Exception details are::{}", e);
            }


        return adminDetails;
    }


    public SubmitElevationResponse submitElevationRequest(SubmitElevationRequest request) {

        return null;
    }
}
