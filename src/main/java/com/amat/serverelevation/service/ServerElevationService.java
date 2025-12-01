package com.amat.serverelevation.service;


import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.accessmanagement.service.RoleService;
import com.amat.admanagement.dto.*;
import com.amat.admanagement.service.ComputerService;
import com.amat.admanagement.service.GroupsService;
import com.amat.admanagement.service.UserService;
import com.amat.serverelevation.DTO.getServerElevationRequests;
import com.amat.serverelevation.DTO.*;
import com.amat.serverelevation.entity.ApprovalDetails;
import com.amat.serverelevation.entity.ServerElevationRequest;
import com.amat.serverelevation.repository.ApprovalDetailsRepository;
import com.amat.serverelevation.repository.ServerElevationRepository;
import com.amat.serverelevation.repository.ServerElevationRequestRepository;
import com.amat.serverelevation.util.ServerElevationUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;


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
    ServerElevationRepository serverRepo;

    @Autowired
    ApprovalDetailsRepository approvalRepo;

    @Autowired
    ServerElevationRequestRepository serverElevationRequestRepository;

    @Autowired
    RoleService roleService;


    @Autowired
    ServerElevationUtils utils;

    @Autowired
    UserEnrollmentRepository userEnrollmentRepository;

    @Value("${spring.ldap.base:''}")
    String defaultBase;



    public ServerElevationResponse validateRequest(ServerElevationRequestDTO request) {
        ServerElevationResponse response = ServerElevationResponse.builder()
                .serverName(request.getServerName())
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

        String userAdminDn = utils.fetchAdminAccountDn(requestorDn);


        if (userAdminDn == null || userAdminDn.isEmpty()) {
            setError(response, "USER_ADMIN_ACCOUNT_NOT_FOUND");
            return response;
        }


        // Step 1: Find Active AD Computer
        ComputersRequest computerReq = ComputersRequest.builder()
                .filter("(&(cn=" + request.getServerName() + ")(!(userAccountControl=4130)))")
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
        String adminGroup = request.getServerName() + "-APP-ADMINS";
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
        String localAdminGroup = request.getServerName() + "-Local-Admins";
        GroupsRequest localGroupReq = GroupsRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(cn=" + localAdminGroup + ")")
                .addtnlAttributes(List.of("member"))
                .fetchRecursiveMembers(false)
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
            GroupsRequest recursiveGroupReq = GroupsRequest.builder()
                    .searchBaseOU(defaultBase)
                    .filter("(cn=" + adminGroup + ")")
                    .pageSize(2)
                    .pageNumber(0)
                    .fetchRecursiveMembers(false)
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

        return response;
    }

    private void setError(ServerElevationResponse response, String errorMsg) {
        response.setEligibleForElevation(false);
        response.setEligibleForElevationMsg(errorMsg);
    }

    @Async
    @Transactional
    public void submitElevationRequest(String employeeId, SubmitElevationRequest request) {
        List<SubmitResponse> results = new ArrayList<>();

        String userDn = utils.fetchUserDn(employeeId);
        String userAdminDn = utils.fetchAdminAccountDn(userDn);

        for (SubmitServerEntry entry : request.getEligibleServers()) {
            String server = entry.getServerName();
            ServerElevationRequestDTO validateReq = ServerElevationRequestDTO.builder()
                    .serverName(server)
                    .requestorEmpId(employeeId)
                    .build();

            // Validate eligibility (additionalDetails = true to get approvalRequired)
            ServerElevationResponse validation = validateRequest(validateReq);

            if (!Boolean.TRUE.equals(validation.getEligibleForElevation())) {
                results.add(new SubmitResponse(server, "Failed", null, null, validation.getEligibleForElevationMsg()));
                continue;
            }

            UUID guid = UUID.randomUUID();
            // Insert server_elevation_requests row (DB will generate RequestID)
            ServerElevationRequest entity = ServerElevationRequest.builder()
                    .requestedBy(employeeId)
                    .serverName(server)
                    .requestId(guid.toString())
                    .durationInHours(entry.getDurationInHours())
                    .requestorComment(request.getComment())
                    .status("In-Progress")
                    .build();

            ServerElevationRequest savedServerEntity =  serverRepo.save(entity);

            String requestId = savedServerEntity.getRequestId();

            if (!Boolean.TRUE.equals(validation.getApprovalRequired())) {
                // No approval: try immediate elevation
                boolean elevationSuccess = false;
                String elevationErr = null;

                try {
                            // 2. Build the group DN for server-local-admins
                            String groupDn = utils.fetchGroupDn(server+ "-LOCAL-ADMINS");

                            // 3. Construct ManageGroupRequest
                            ManageGroupRequest manageReq = new ManageGroupRequest();
                            manageReq.setGroupDn(groupDn);
                            manageReq.setUserDns(List.of(userAdminDn));
                            manageReq.setOperation("ADD");   // ADD / REMOVE

                            // 4. Call groupService
                            ModifyGroupResponse modifyResp = groupsService.modifyGroupMembers(manageReq);

                            if ("200".equals(modifyResp.getStatusCode()) || "success".equalsIgnoreCase(modifyResp.getStatusCode())) {
                                elevationSuccess = true;
                            } else {
                                elevationSuccess = false;
                                elevationErr = String.join(", ", modifyResp.getErrors());
                            }

                } catch (Exception ex) {
                    log.error("Elevation failed for server {}: {}", server, ex.getMessage(), ex);
                    elevationSuccess = false;
                    elevationErr = ex.getMessage();
                }


                if (elevationSuccess) {
                    // Update DB row: elevation success
                    serverRepo.updateOnSuccess(
                            requestId,
                            LocalDateTime.now(),
                            "Success",
                            "Elevated Successfully",
                            "Completed");
                    results.add(new SubmitResponse(server, "Success", requestId, null, "Elevated Successfully"));
                } else {
                    serverRepo.updateOnFailure(requestId, "Failed", elevationErr != null ? elevationErr : "Elevation Failed", "Completed");
                    results.add(new SubmitResponse(server, "Failed", requestId, null, elevationErr != null ? elevationErr : "Elevation Failed"));
                }
            } else {
                // Approval required: create approval_details row (DB will generate ApprovalID)
                ApprovalDetails approval = ApprovalDetails.builder()
                        .requestId(requestId)
                        .approver(validation.getOwnerDetails().getOwnerEmpID())
                        .approverName(getOwnDisplayName(validation.getOwnerDetails().getOwnerEmpID()))
                        .workItemName(server + " (Duration: " + entry.getDurationInHours() + " Hours)")
                        .workItemType("SERVER-ELEVATION")
                        .approvalStatus("Pending-Approval")
                        .build();

                approvalRepo.save(approval);
                approvalRepo.flush();

                String approvalId = String.valueOf(approval.getApprovalId());
                if (approvalId == null) {
                    approvalId = String.valueOf(approval.getApprovalId());
                }

                serverRepo.updateStatusAndApprover(requestId, "Pending-Approval", approvalId);
                results.add(new SubmitResponse(server, "Pending-Approval", requestId, approvalId, "Waiting for Owner Approval"));
            }
        }

        log.info("results list::{}", results);

    }


    public Object getRequests(getServerElevationRequests serverEleReq,
                                                    String loggedInUser,
                                                    boolean isSelf,
                                                    Pageable pageable) {

        if (!isSelf) {
            boolean isAdmin = roleService.hasRole(loggedInUser, "ServerElevation-Administrator");
            if (!isAdmin) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "status", "FAILED",
                                "message", "Access Denied: You are not authorized to view this information"
                        ));
            }
        }

        if(serverEleReq.getFilter().getRequestorName()!=null && !serverEleReq.getFilter().getRequestorName().isBlank()){
            loggedInUser = serverEleReq.getFilter().getRequestorName();
            isSelf=true;
        }


        Specification<ServerElevationRequest> spec =
                ServerElevationRequestSpecification.applyFilters(serverEleReq, loggedInUser, isSelf);

        Page<ServerElevationRequest> pageData = serverElevationRequestRepository.findAll(spec, pageable);

        // 🔽 Sorting by approval date if approval exists
        List<String> requestIds = pageData.getContent().stream()
                .map(ServerElevationRequest::getRequestId)
                .toList();

        List<ApprovalDetails> approvals =
                approvalRepo.findByRequestIdInOrderByApprovalRequestDateDesc(requestIds);

        // No mapping required — just sorted correctly if present
        return ResponseEntity.ok(pageData);
    }

    private String getOwnDisplayName(String ownerEmpId) {
        return userEnrollmentRepository.findById(ownerEmpId)
                .map(UserEntity::getDisplayName)
                .orElse("");
    }


}
