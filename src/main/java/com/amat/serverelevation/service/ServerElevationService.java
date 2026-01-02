package com.amat.serverelevation.service;

import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.accessmanagement.repository.UserPreferencesRepository;
import com.amat.accessmanagement.service.RoleService;
import com.amat.admanagement.dto.*;
import com.amat.admanagement.service.ComputerService;
import com.amat.admanagement.service.GroupsService;
import com.amat.admanagement.service.UserService;
import com.amat.approvalmanagement.enums.ApprovalStatus;
import com.amat.commonutils.entity.UserPreferences;
import com.amat.commonutils.utis.CommonUtils;
import com.amat.serverelevation.DTO.getServerElevationRequests;
import com.amat.serverelevation.DTO.*;
import com.amat.approvalmanagement.entity.ApprovalDetails;
import com.amat.serverelevation.entity.ServerElevationRequest;
import com.amat.approvalmanagement.repository.ApprovalDetailsRepository;
import com.amat.serverelevation.repository.ServerElevationRepository;
import com.amat.serverelevation.repository.ServerElevationRequestRepository;
import com.amat.serverelevation.util.ServerElevationUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    UserPreferencesRepository userPreferencesRepository;

    @Autowired
    CommonUtils commonUtils;

    @Value("${spring.ldap.base:''}")
    String defaultBase;

    public ServerElevationResponse validateRequest(ServerElevationRequestDTO request) {

        log.info("START validateRequest | serverName={} | requestorEmpId={}",
                request.getServerName(), request.getRequestorEmpId());

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

        log.debug("Fetching requestor from AD | employeeId={}", request.getRequestorEmpId());

        Map<String, Object> requestorResp = userService.fetchAllObjects(requestorReq);
        List<Map<String, Object>> requestorData =
                (List<Map<String, Object>>) requestorResp.get("data");

        if (requestorData == null || requestorData.isEmpty()) {
            log.warn("USER_NOT_FOUND | employeeId={}", request.getRequestorEmpId());
            setError(response, "USER_NOT_FOUND");
            return response;
        }

        String requestorDn = (String) requestorData.get(0).get("distinguishedName");
        String userAdminDn = utils.fetchAdminAccountDn(requestorDn);

        log.debug("Resolved DNs | requestorDn={} | adminDn={}", requestorDn, userAdminDn);

        if (userAdminDn == null || userAdminDn.isEmpty()) {
            log.warn("USER_ADMIN_ACCOUNT_NOT_FOUND | requestorDn={}", requestorDn);
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

        log.debug("Fetching computer from AD | serverName={}", request.getServerName());

        Map<String, Object> computerResult = computerService.fetchAllObjects(computerReq);
        List<Map<String, Object>> computers =
                (List<Map<String, Object>>) computerResult.get("data");

        if (computers == null || computers.isEmpty()) {
            log.warn("SERVER_NOT_FOUND | serverName={}", request.getServerName());
            setError(response, "SERVER_NOT_FOUND");
            return response;
        }

        Map<String, Object> computer = computers.get(0);
        response.setOperatingSystem((String) computer.get("operatingSystem"));
        response.setApplicationName((String) computer.get("facsimileTelephoneNumber"));

        // Step 2 & 3: Find Admin Group
        String adminGroup = request.getServerName() + "-APP-ADMINS";

        GroupsRequest groupReq = GroupsRequest.builder()
                .searchBaseOU(defaultBase)
                .filter("(cn=" + adminGroup + ")")
                .addtnlAttributes(List.of("managedBy"))
                .pageNumber(0)
                .pageSize(1)
                .build();

        log.debug("Fetching admin group | groupName={}", adminGroup);

        Map<String, Object> groupResult = groupsService.fetchAllGroups(groupReq);
        List<Map<String, Object>> groups =
                (List<Map<String, Object>>) groupResult.get("data");

        if (groups == null || groups.isEmpty()) {
            log.warn("APP_ADMIN_GROUP_NOT_FOUND | group={}", adminGroup);
            setError(response, "APP_ADMIN_GROUP_NOT_FOUND");
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

        log.debug("Fetching owner info | ownerDn={}", ownerDn);

        Map<String, Object> userResult = userService.fetchAllObjects(userReq);
        List<Map<String, Object>> users =
                (List<Map<String, Object>>) userResult.get("data");

        if (users == null || users.isEmpty()) {
            log.warn("OWNER_NOT_FOUND | ownerDn={}", ownerDn);
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

        log.debug("Fetching local admin group | groupName={}", localAdminGroup);

        Map<String, Object> localGroupResult = groupsService.fetchAllGroups(localGroupReq);
        List<Map<String, Object>> localGroups =
                (List<Map<String, Object>>) localGroupResult.get("data");

        if (localGroups == null || localGroups.isEmpty()) {
            log.warn("ELEVATION_GROUP_NOT_FOUND | group={}", localAdminGroup);
            setError(response, "ELEVATION_GROUP_NOT_FOUND");
            return response;
        }

        Object memberObj = localGroups.get(0).get("member");

        List<String> members =
                memberObj instanceof List
                        ? (List<String>) memberObj
                        : Collections.singletonList(String.valueOf(memberObj));

        if (members != null && members.contains(userAdminDn)) {
            log.warn("USER_ALREADY_ELEVATED | userAdminDn={}", userAdminDn);
            response.setEligibleForElevation(false);
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

        log.debug("Checking approval requirement | adminGroup={}", adminGroup);

        Map<String, Object> recursiveGroupResult = groupsService.fetchAllGroups(recursiveGroupReq);
        List<Map<String, Object>> recursiveGroupResultData =
                (List<Map<String, Object>>) recursiveGroupResult.get("data");

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

        if (response.getApprovalRequired()) {
            OwnerDetails ownerDetails = response.getOwnerDetails();

            if (ownerDetails == null ||
                    ownerDetails.getOwnerEmpID() == null ||
                    ownerDetails.getOwnerEmpID().isBlank()) {

                log.warn(
                        "OWNER_EMP_ID_MISSING | server={} | approvalRequired=true",
                        request.getServerName()
                );

                response.setEligibleForElevation(false);
                setError(response, "OWNER_EMP_ID_MISSING");
                return response;
            }
        }

        log.info("END validateRequest | server={} | eligible={} | approvalRequired={}",
                request.getServerName(),
                response.getEligibleForElevation(),
                response.getApprovalRequired());

        return response;
    }

    private void setError(ServerElevationResponse response, String errorMsg) {
        log.warn("Validation failed | error={}", errorMsg);
        response.setEligibleForElevation(false);
        response.setEligibleForElevationMsg(errorMsg);
    }

    @Async
    @Transactional
    public void submitElevationRequest(String employeeId, SubmitElevationRequest request) {

        log.info("START submitElevationRequest | employeeId={} | servers={}",
                employeeId, request.getEligibleServers().size());

        List<SubmitResponse> results = new ArrayList<>();

        String userDn = utils.fetchUserDn(employeeId);
        String userAdminDn = utils.fetchAdminAccountDn(userDn);

        for (SubmitServerEntry entry : request.getEligibleServers()) {

            String server = entry.getServerName();
            log.info("Processing server elevation | server={}", server);

            ServerElevationRequestDTO validateReq = ServerElevationRequestDTO.builder()
                    .serverName(server)
                    .requestorEmpId(employeeId)
                    .build();

            // Validate eligibility (additionalDetails = true to get approvalRequired)
            ServerElevationResponse validation = validateRequest(validateReq);

            if (!Boolean.TRUE.equals(validation.getEligibleForElevation())) {
                log.warn("Server not eligible | server={} | reason={}",
                        server, validation.getEligibleForElevationMsg());
                results.add(new SubmitResponse(server, "Failed", null, null,
                        validation.getEligibleForElevationMsg()));
                continue;
            }

            UUID guid = UUID.randomUUID();
            // Insert server_elevation_requests row (DB will generate RequestID)
            ServerElevationRequest entity = ServerElevationRequest.builder()
                    .requestedBy(employeeId)
                    .serverName(server)
                    .requestId(guid.toString().toUpperCase())
                    .durationInHours(entry.getDurationInHours())
                    .requestorComment(request.getComment())
                    .status("In-Progress")
                    .build();

            ServerElevationRequest savedServerEntity = serverRepo.save(entity);
            String requestId = savedServerEntity.getRequestId();

            log.info("Request saved | server={} | requestId={}", server, requestId);

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
                            requestId.toUpperCase(),
                            LocalDateTime.now(),
                            "Success",
                            "Elevated Successfully",
                            ApprovalStatus.In_Progress.name());

                    results.add(new SubmitResponse(server, "Success", requestId, null, "Elevated Successfully"));
                } else {
                    serverRepo.updateOnFailure(requestId, "Failed", elevationErr != null ? elevationErr : "Elevation Failed", "Completed");
                    results.add(new SubmitResponse(server, "Failed", requestId, null, elevationErr != null ? elevationErr : "Elevation Failed"));
                }
            } else {
                String approverEmpId = validation.getOwnerDetails().getOwnerEmpID();
                String finalApprover = approverEmpId;

                // Fetch preferences of owner
                Optional<UserPreferences> ownerPrefsOpt =
                        userPreferencesRepository.findById(approverEmpId);

                if (ownerPrefsOpt.isPresent()) {
                    UserPreferences prefs = ownerPrefsOpt.get();

                    if (commonUtils.isUserOutOfOffice(prefs)) {
                        if (prefs.getOooApprover() != null && !prefs.getOooApprover().isBlank()) {
                            log.info(
                                    "Owner {} is OOO ({} to {}). Reassigning approval to alternate approver {}",
                                    approverEmpId,
                                    prefs.getOooStartDate(),
                                    prefs.getOooEndDate(),
                                    prefs.getOooApprover()
                            );

                            finalApprover = prefs.getOooApprover();
                        } else {
                            log.warn(
                                    "Owner {} is OOO but no alternate approver configured. Using original approver.",
                                    approverEmpId
                            );
                        }
                    }
                }


                // Approval required: create approval_details row (DB will generate ApprovalID)
                ApprovalDetails approval = ApprovalDetails.builder()
                        .requestId(requestId)
                        .approver(finalApprover)
                        .workItemName(server + " (Duration: " + entry.getDurationInHours() + " Hours)")
                        .workItemType("SERVER-ELEVATION")
                        .approvalStatus(ApprovalStatus.Pending_Approval.name())
                        .approvalLevel(1)
                        .requestee(employeeId)
                        .requestor(employeeId)
                        .build();


                approvalRepo.save(approval);
                approvalRepo.flush();

                String approvalId = String.valueOf(approval.getApprovalId());
                if (approvalId == null) {
                    approvalId = String.valueOf(approval.getApprovalId());
                }

                serverRepo.updateStatusAndApprover(requestId, ApprovalStatus.Pending_Approval.name(), approvalId.toUpperCase());
                results.add(new SubmitResponse(server, ApprovalStatus.Pending_Approval.name(), requestId, approvalId.toUpperCase(), finalApprover.equals(approverEmpId)
                        ? "Waiting for Owner Approval"
                        : "Waiting for Alternate Approver Approval"));
            }
        }

        log.info("END submitElevationRequest | results={}", results);
    }

    public Object getRequests(getServerElevationRequests serverEleReq,
                              String loggedInUser,
                              boolean isSelf,
                              Pageable pageable) {

        log.info("START getRequests | user={} | isSelf={}", loggedInUser, isSelf);

        if (!isSelf) {
            boolean isAdmin = roleService.hasRole(loggedInUser, "ServerElevation-Administrator");
            log.debug("Admin check | user={} | isAdmin={}", loggedInUser, isAdmin);

            if (!isAdmin) {
                log.warn("Access denied | user={}", loggedInUser);
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "status", "FAILED",
                                "message", "Access Denied: You are not authorized to view this information"
                        ));
            }
        }

        if (serverEleReq.getFilter().getRequestorName() != null
                && !serverEleReq.getFilter().getRequestorName().isBlank()) {

            log.debug("Requestor override | originalUser={} | overriddenUser={}",
                    loggedInUser, serverEleReq.getFilter().getRequestorName());

            loggedInUser = serverEleReq.getFilter().getRequestorName();
            isSelf = true;
        }

        Specification<ServerElevationRequest> spec =
                ServerElevationRequestSpecification.applyFilters(serverEleReq, loggedInUser, isSelf);

        Page<ServerElevationRequest> pageData =
                serverElevationRequestRepository.findAll(spec, pageable);

// Extract requestIds
        List<String> requestIds = pageData.getContent()
                .stream()
                .map(ServerElevationRequest::getRequestId)
                .toList();

// Fetch approvals once
        List<ApprovalDetails> approvals =
                approvalRepo.findByRequestIdInOrderByApprovalRequestDateDesc(requestIds);

// Group approvals by requestId
        Map<String, List<ApprovalDetails>> approvalMap =
                approvals.stream()
                        .collect(Collectors.groupingBy(ApprovalDetails::getRequestId));

// Map Entity → DTO
        Page<ServerElevationRequestResponse> responsePage =
                pageData.map(req -> ServerElevationRequestResponse.builder()
                        .id(req.getId())
                        .requestId(req.getRequestId())
                        .requestedBy(req.getRequestedBy())
                        .serverName(req.getServerName())
                        .durationInHours(req.getDurationInHours())
                        .requestorComment(req.getRequestorComment())
                        .requestDate(req.getRequestDate())
                        .elevationTime(req.getElevationTime())
                        .elevationStatus(req.getElevationStatus())
                        .deElevationTime(req.getDeElevationTime())
                        .deElevationStatus(req.getDeElevationStatus())
                        .elevationStatusMessage(req.getElevationStatusMessage())
                        .deElevationStatusMessage(req.getDeElevationStatusMessage())
                        .status(req.getStatus())
                        .approvalId(req.getApprovalId())
                        .approvalDetails(
                                approvalMap.getOrDefault(req.getRequestId(), List.of())
                        )
                        .build()
                );

        return ResponseEntity.ok(responsePage);


        // No mapping required — just sorted correctly if present
//        return ResponseEntity.ok(pageData);
    }

    private String getOwnDisplayName(String ownerEmpId) {
        log.debug("Fetching displayName | ownerEmpId={}", ownerEmpId);
        return userEnrollmentRepository.findById(ownerEmpId)
                .map(UserEntity::getDisplayName)
                .orElse("");
    }

    @Transactional
    public void performPostApprovalDenyActionServerElevation(String loggedInUser, String requestId) {

        log.info("START performPostApprovalDenyActionServerElevation | requestId={} | user={}",
                requestId, loggedInUser);
        // 1. Fetch server elevation request
        ServerElevationRequest req = serverElevationRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Server elevation request not found: " + requestId));

        String server = req.getServerName();
        String requestee = req.getRequestedBy();
        // 2. Resolve admin account DN
        log.debug("Resolved request | server={} | requestee={}", server, requestee);

        String userDn = utils.fetchUserDn(loggedInUser);
        log.info("Adding to ADMINS-GROUPS | userDn={}", userDn);
        String userAdminDn = utils.fetchAdminAccountDn(userDn);
        log.info("Adding to APP-GROUPS | userAdminDn={}", userAdminDn);
        boolean appAdminSuccess = false;
        boolean localAdminSuccess = false;
        String errorMsg = null;

        try {
            log.info("Adding to APP-ADMINS | server={}", server);

            String appAdminsGroupDn = utils.fetchGroupDn(server + "-APP-ADMINS");
            log.info("Adding to APP-ADMINS | appAdminsGroupDn={}", appAdminsGroupDn);
            ManageGroupRequest appAdminReq = new ManageGroupRequest();
            appAdminReq.setGroupDn(appAdminsGroupDn);
            appAdminReq.setUserDns(List.of(userAdminDn));
            appAdminReq.setOperation("ADD");

            ModifyGroupResponse appResp = groupsService.modifyGroupMembers(appAdminReq);
            log.info("Adding to APP-ADMINS | addToAppAdminsResp={}", appResp);
            if ("200".equals(appResp.getStatusCode())
                    || "success".equalsIgnoreCase(appResp.getStatusCode())) {
                appAdminSuccess = true;
            } else {
                errorMsg = String.join(", ", appResp.getErrors());
                log.error("Add operation for user ::{} | with appAdminsGroupDn {} | to APP-ADMINS Failed with errors ::{}",userDn, appAdminsGroupDn,errorMsg);
            }


                log.info("Adding to LOCAL-ADMINS | server={}", server);

                String localAdminsGroupDn = utils.fetchGroupDn(server + "-LOCAL-ADMINS");
                log.info("Adding to LOCAL-ADMINS | localAdminsGroupDn={}", appAdminsGroupDn);
                ManageGroupRequest localAdminReq = new ManageGroupRequest();
                localAdminReq.setGroupDn(localAdminsGroupDn);
                localAdminReq.setUserDns(List.of(userAdminDn));
                localAdminReq.setOperation("ADD");

                ModifyGroupResponse localResp = groupsService.modifyGroupMembers(localAdminReq);
                log.info("Adding to APP-ADMINS | addToLocalAdminsResp={}", localResp);
                if ("200".equals(localResp.getStatusCode())
                        || "success".equalsIgnoreCase(localResp.getStatusCode())) {
                    localAdminSuccess = true;
                    log.info("Updating server elevation table::Start");
                    serverElevationRequestRepository.findByRequestId(requestId).ifPresent(serReq -> {
                        serReq.setElevationStatus("Success");
                        serReq.setElevationTime(LocalDateTime.now());
                        serReq.setElevationStatusMessage("Elevated Successfully");
                        serReq.setStatus(ApprovalStatus.In_Progress.name());
                    });
                    log.info("Updating server elevation table::Done");

                } else {
                    errorMsg = String.join(", ", localResp.getErrors());
                    log.error("Add operation for user ::{} | with localAdminsGroupDn {} | to APP-ADMINS Failed with errors ::{}",userDn, localAdminsGroupDn,errorMsg);

                    serverRepo.updateOnFailure(
                            requestId,
                            "Failed",
                             errorMsg != null ? errorMsg : "Group assignment failed",
                            "Completed"
                    );

                }

        } catch (Exception ex) {
            log.error("Post-approval elevation failed | requestId={} | error={}",
                    requestId, ex.getMessage(), ex);
        }

        log.info("END performPostApprovalDenyActionServerElevation | requestId={}", requestId);
    }

    @Transactional
    public void cancelRequest(
            CancelServerElevationRequest req,
            String loggedInUser
    ) {

        log.info(
                "START :: Cancel server elevation | requestId={} | user={}",
                req.getRequestId(),
                loggedInUser
        );

        if (req.getRequestId() == null || req.getRequestId().isBlank()) {
            log.warn("Invalid requestId");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "RequestId must not be empty"
            );
        }

        try {
            // 1. Fetch server elevation request
            ServerElevationRequest request = serverRepo
                    .findByRequestId(req.getRequestId())
                    .orElseThrow(() -> {
                        log.warn("Server elevation request not found | requestId={}", req.getRequestId());
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Server elevation request not found"
                        );
                    });

            // 2. Validate status
            if (!"Pending_Approval".equalsIgnoreCase(request.getStatus())) {
                log.warn(
                        "Cancel not allowed | requestId={} | status={}",
                        req.getRequestId(),
                        request.getStatus()
                );
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only Pending_Approval requests can be cancelled"
                );
            }

            // 3. Update server_elevation_requests
            request.setStatus(ApprovalStatus.Cancelled.name());
            request.setElevationStatusMessage(
                    "Request cancelled by " + loggedInUser +
                            (req.getComment() != null ? " : " + req.getComment() : "")
            );

            serverRepo.save(request);

            log.info(
                    "Server elevation request marked CANCELLED | requestId={}",
                    req.getRequestId()
            );

            // 4. Update approval_details
            int updatedRows = approvalRepo.cancelApprovals(
                    req.getRequestId(),
                    "Request cancelled by " + loggedInUser
            );



            log.info(
                    "Approval records cancelled | requestId={} | rowsUpdated={}",
                    req.getRequestId(),
                    updatedRows
            );

        } catch (ResponseStatusException ex) {
            log.error(
                    "BUSINESS ERROR :: Cancel request failed | requestId={} | reason={}",
                    req.getRequestId(),
                    ex.getReason()
            );
            throw ex;

        } catch (Exception ex) {
            log.error(
                    "SYSTEM ERROR :: Cancel request failed | requestId={}",
                    req.getRequestId(),
                    ex
            );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to cancel server elevation request"
            );
        } finally {
            log.info(
                    "END :: Cancel server elevation | requestId={}",
                    req.getRequestId()
            );
        }
    }


}
