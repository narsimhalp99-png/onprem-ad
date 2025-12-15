package com.amat.approvalmanagement.service;



import com.amat.accessmanagement.service.RoleService;
import com.amat.admanagement.dto.ManageGroupRequest;
import com.amat.admanagement.dto.ModifyGroupResponse;
import com.amat.admanagement.service.GroupsService;
import com.amat.approvalmanagement.dto.ApprovalActionRequest;
import com.amat.approvalmanagement.dto.ApprovalDetailsFilterDTO;
import com.amat.approvalmanagement.enums.ApprovalStatus;
import com.amat.approvalmanagement.repository.ApprovalDetailsFilterRepository;
import com.amat.serverelevation.entity.ApprovalDetails;
import com.amat.serverelevation.entity.ServerElevationRequest;
import com.amat.serverelevation.repository.ApprovalDetailsRepository;
import com.amat.serverelevation.repository.ServerElevationRepository;
import com.amat.serverelevation.repository.ServerElevationRequestRepository;
import com.amat.serverelevation.util.ServerElevationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
public class ApprovalsService {


    @Autowired
    ApprovalDetailsFilterRepository approvalRepo;

    @Autowired
    ApprovalDetailsRepository approvalDetailsRepository;

    @Autowired
    ServerElevationRequestRepository serverElevationRequestRepository;

    @Autowired
    ServerElevationRepository serverRepo;


    @Autowired
    RoleService roleService;

    @Autowired
    ServerElevationUtils utils;

    @Autowired
    GroupsService groupsService;


    public Object getApprovalDetails(ApprovalDetailsFilterDTO filter, int page, int size, String loggedInUser, boolean isSelf) {

        if (!isSelf) {
            boolean isAdmin = roleService.hasRole(loggedInUser, "Approval-Administrator");
            if (!isAdmin) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "status", "FAILED",
                                "message", "Access Denied: You are not authorized to view this information"
                        ));
            }
        }

        if(filter.getApprover()!=null && !filter.getApprover().isBlank()){
            loggedInUser = filter.getApprover();
            isSelf=true;
        }


        String validSortField = (filter.getSortField() != null && !filter.getSortField() .isEmpty())
                ? filter.getSortField()
                : "approvalRequestDate";

        Sort.Direction direction = (filter.getSortDirection()  != null && filter.getSortDirection().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortField));


        Specification<ApprovalDetails> spec = ApprovalDetailsRequestSpecification.applyFilters(filter, loggedInUser, isSelf);


        return approvalRepo.findAll(spec, pageable);
    }


    @Transactional
    public void approveOrReject(ApprovalActionRequest req, String loggedInUser) {
        ApprovalDetails approval = new ApprovalDetails();
        Optional<ApprovalDetails> approvals =
                approvalDetailsRepository.findByApprovalIdAndApproverAndApprovalStatus(
                        req.getApprovalId(),
                        loggedInUser,
                        "Pending-Approval"
                );

        if (approvals.isPresent()){
            approval = approvals.get();
            int level = approval.getApprovalLevel();
            String requestId = approval.getRequestId();

            // Update current approval
            approval.setApprovalDate(LocalDateTime.now());
            approval.setApprovalStatus(req.isApprove()
                    ? ApprovalStatus.APPROVED.name()
                    : ApprovalStatus.DENIED.name());
            approval.setApproverComment(req.getComment());
            // Mark parallel approvals
            markParallelApprovals(requestId, level, loggedInUser, req.getAction());
        }

        if (req.isApprove()) {
            handleNextLevelOrPostAction(loggedInUser,approval.getRequestId(), approval.getApprovalLevel(),  approval.getWorkItemType());
        } else {
            cancelFutureApprovals(approval.getRequestId(), approval.getApprovalLevel());
        }
    }

    private void markParallelApprovals(
            String requestId,
            int level,
            String user,
            String action) {

        List<ApprovalDetails> approvals =
                approvalDetailsRepository.findByRequestIdAndApprovalLevelAndApprovalStatus(
                        requestId,
                        level,
                        "Pending-Approval");

        approvals.forEach(a -> {
            a.setApprovalStatus("Not-Required");
            a.setApproverComment(action + " by " + user);
        });
    }

    private void handleNextLevelOrPostAction(
            String loggedInUser,
            String requestId,
            int level,
            String workItemType) {

        List<ApprovalDetails> next =
                approvalDetailsRepository.findByRequestIdAndApprovalStatusAndApprovalLevel(
                        requestId,
                        "Not-Started",
                        level + 1);

        if (!next.isEmpty()) {
            next.forEach(a ->
                    a.setApprovalStatus("Pending-Approval"));
        } else {
            performPostApprovalAction(loggedInUser,requestId, workItemType);
        }
    }

    private void cancelFutureApprovals(String requestId, int level) {

        List<ApprovalDetails> future =
                approvalDetailsRepository.findByRequestIdAndApprovalStatusAndApprovalLevelGreaterThan(
                        requestId,
                        "Not-Started",
                        level);

        future.forEach(a -> {
            a.setApprovalStatus(ApprovalStatus.CANCELLED.name());
            a.setApproverComment("Not-Required");
        });
    }

    private void performPostApprovalAction(String loggedInUser,String requestId, String workItemType) {

        if ("SERVER-ELEVATION".equalsIgnoreCase(workItemType)) {
            performPostApprovalDenyActionServerElevation(loggedInUser,requestId);
            serverElevationRequestRepository.findByRequestId(requestId).ifPresent(req -> {
                req.setElevationStatus("Success");
                req.setElevationTime(LocalDateTime.now());
                req.setElevationStatusMessage("Elevated Successfully");
            });
        }
    }

    @Transactional
    private void performPostApprovalDenyActionServerElevation(String loggedInUser,String requestId) {

        // 1. Fetch server elevation request
        ServerElevationRequest req = serverElevationRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Server elevation request not found: " + requestId));

        String server = req.getServerName();
        String requestee = req.getRequestedBy();

        // 2. Resolve admin account DN

        String userDn = utils.fetchUserDn(loggedInUser);
        String userAdminDn = utils.fetchAdminAccountDn(userDn);

        boolean appAdminSuccess = false;
        boolean localAdminSuccess = false;
        String errorMsg = null;

        try {
            // Add to SERVER-APP-ADMINS
            String appAdminsGroupDn = utils.fetchGroupDn(server + "-APP-ADMINS");

            ManageGroupRequest appAdminReq = new ManageGroupRequest();
            appAdminReq.setGroupDn(appAdminsGroupDn);
            appAdminReq.setUserDns(List.of(userAdminDn));
            appAdminReq.setOperation("ADD");

            ModifyGroupResponse appResp = groupsService.modifyGroupMembers(appAdminReq);

            if ("200".equals(appResp.getStatusCode())
                    || "success".equalsIgnoreCase(appResp.getStatusCode())) {
                appAdminSuccess = true;
            } else {
                errorMsg = String.join(", ", appResp.getErrors());
            }


            // Add to SERVER-LOCAL-ADMINS
            if (appAdminSuccess) {
                String localAdminsGroupDn = utils.fetchGroupDn(server + "-LOCAL-ADMINS");

                ManageGroupRequest localAdminReq = new ManageGroupRequest();
                localAdminReq.setGroupDn(localAdminsGroupDn);
                localAdminReq.setUserDns(List.of(userAdminDn));
                localAdminReq.setOperation("ADD");

                ModifyGroupResponse localResp = groupsService.modifyGroupMembers(localAdminReq);

                if ("200".equals(localResp.getStatusCode())
                        || "success".equalsIgnoreCase(localResp.getStatusCode())) {
                    localAdminSuccess = true;
                } else {
                    errorMsg = String.join(", ", localResp.getErrors());
                }
            }

        } catch (Exception ex) {
            log.error("Post-approval elevation failed for requestId {}: {}", requestId, ex.getMessage(), ex);
            errorMsg = ex.getMessage();
        }


        if (appAdminSuccess && localAdminSuccess) {

            serverRepo.updateOnSuccess(
                    requestId,
                    LocalDateTime.now(),
                    "Approved",
                    "Admin access granted",
                    "Completed"
            );

           // Elevation service success email

        } else {

            serverRepo.updateOnFailure(
                    requestId,
                    "Failed",
                    errorMsg != null ? errorMsg : "Group assignment failed",
                    "Completed"
            );

            // Elevation service Failure Email
        }
    }


}

