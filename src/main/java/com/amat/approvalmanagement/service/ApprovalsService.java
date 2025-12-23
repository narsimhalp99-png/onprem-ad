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
import com.amat.serverelevation.service.ServerElevationService;
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

    @Autowired
    ServerElevationService serverElevationService;


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
                        ApprovalStatus.Pending_Approval.name()
                );
        String requestedBy = "";
        if (approvals.isPresent()){
            approval = approvals.get();
            int level = approval.getApprovalLevel();
            String requestId = approval.getRequestId();

            // Update current approval
            approval.setApprovalDate(LocalDateTime.now());
            approval.setApprovalStatus(req.isApprove()
                    ? ApprovalStatus.Approved.name()
                    : ApprovalStatus.Denied.name());
            approval.setApproverComment(req.getComment());

            Optional<ServerElevationRequest> serverElevationRequest = serverElevationRequestRepository.findByRequestId(requestId);
            requestedBy = serverElevationRequest
                    .map(ServerElevationRequest::getRequestedBy)
                    .orElse("");


            // Mark parallel approvals
            markParallelApprovals(requestId, level, requestedBy, req.getAction());

        }

        if (req.isApprove()) {
            handleNextLevelOrPostAction(requestedBy,approval.getRequestId(), approval.getApprovalLevel(),  approval.getWorkItemType());
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
                        ApprovalStatus.Pending_Approval.name());

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
                    a.setApprovalStatus(ApprovalStatus.Pending_Approval.name()));
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
            a.setApprovalStatus(ApprovalStatus.Cancelled.name());
            a.setApproverComment("Not-Required");
        });
    }

    private void performPostApprovalAction(String loggedInUser,String requestId, String workItemType) {

        if ("SERVER-ELEVATION".equalsIgnoreCase(workItemType)) {
            serverElevationService.performPostApprovalDenyActionServerElevation(loggedInUser,requestId);

        }
    }



}

