package com.amat.approvalmanagement.service;

import com.amat.accessmanagement.service.RoleService;
import com.amat.admanagement.service.GroupsService;
import com.amat.approvalmanagement.dto.ApiResponse;
import com.amat.approvalmanagement.dto.ApprovalActionRequest;
import com.amat.approvalmanagement.dto.ApprovalDetailsFilterDTO;
import com.amat.approvalmanagement.enums.ApprovalStatus;
import com.amat.approvalmanagement.repository.ApprovalDetailsFilterRepository;
import com.amat.approvalmanagement.entity.ApprovalDetails;
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

        log.info(
                "START getApprovalDetails | user={} | page={} | size={} | isSelf={} | filter={}",
                loggedInUser, page, size, isSelf, filter
        );

        if (!isSelf) {
            log.debug("Non-self request detected, validating admin role | user={}", loggedInUser);
            boolean isAdmin = roleService.hasRole(loggedInUser, "Approval-Administrator");
            log.debug("Admin role validation result | user={} | isAdmin={}", loggedInUser, isAdmin);

            if (!isAdmin) {
                log.warn("Access denied for user={} while fetching approval details", loggedInUser);
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "status", "FAILED",
                                "message", "Access Denied: You are not authorized to view this information"
                        ));
            }
        }

        if (filter.getApprover() != null && !filter.getApprover().isBlank()) {
            log.debug(
                    "Approver override detected | originalUser={} | overriddenUser={}",
                    loggedInUser,
                    filter.getApprover()
            );
            loggedInUser = filter.getApprover();
            isSelf = true;
        }

        String validSortField = (filter.getSortField() != null && !filter.getSortField().isEmpty())
                ? filter.getSortField()
                : "approvalRequestDate";

        Sort.Direction direction = (filter.getSortDirection() != null && filter.getSortDirection().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        log.debug(
                "Pagination & sorting resolved | sortField={} | direction={} | page={} | size={}",
                validSortField, direction, page, size
        );

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortField));

        Specification<ApprovalDetails> spec =
                ApprovalDetailsRequestSpecification.applyFilters(filter, loggedInUser, isSelf);

        log.debug("Specification built successfully | user={} | isSelf={}", loggedInUser, isSelf);

        Object response = approvalRepo.findAll(spec, pageable);

        log.info("END getApprovalDetails | user={} | resultReturned", loggedInUser);

        return response;
    }

    @Transactional
    public void approveOrReject(ApprovalActionRequest req, String loggedInUser) {

        log.info(
                "START approveOrReject | approvalId={} | action={} | user={}",
                req.getApprovalId(),
                req.getAction(),
                loggedInUser
        );

        Optional<ApprovalDetails> approvals =
                approvalDetailsRepository.findByApprovalIdAndApproverAndApprovalStatus(
                        req.getApprovalId(),
                        loggedInUser,
                        ApprovalStatus.Pending_Approval.name()
                );

        log.debug(
                "Approval lookup completed | approvalId={} | found={}",
                req.getApprovalId(),
                approvals.isPresent()
        );

        if (approvals.isEmpty()) {
            log.warn(
                    "Approval not found or not in pending state | approvalId={} | user={}",
                    req.getApprovalId(),
                    loggedInUser
            );
            throw new IllegalStateException("Approval not found or already processed");
        }

        ApprovalDetails approval = approvals.get();

        if (!approval.getApprover().equalsIgnoreCase(loggedInUser)) {
            log.warn(
                    "User not authorized to approve | approvalId={} | approver={} | loggedInUser={}",
                    approval.getApprovalId(),
                    approval.getApprover(),
                    loggedInUser
            );
            throw new AccessDeniedException("Access Denied: You are not authorized to perform this action");
        }

        int level = approval.getApprovalLevel();
        String requestId = approval.getRequestId();

        log.debug(
                "Approval context resolved | requestId={} | level={} | workItemType={}",
                requestId,
                level,
                approval.getWorkItemType()
        );

        approval.setApprovalDate(LocalDateTime.now());
        approval.setApprovalStatus(
                req.isApprove()
                        ? ApprovalStatus.Approved.name()
                        : ApprovalStatus.Denied.name()
        );
        approval.setApproverComment(req.getComment());

        log.info(
                "Approval updated | approvalId={} | status={} | level={}",
                approval.getApprovalId(),
                approval.getApprovalStatus(),
                level
        );

        String requestedBy =
                serverElevationRequestRepository.findByRequestId(requestId)
                        .map(ServerElevationRequest::getRequestedBy)
                        .orElse("");

        log.debug(
                "Resolved request owner | requestId={} | requestedBy={}",
                requestId,
                requestedBy
        );

        // Handle parallel approvals
        makeParallelApprovals(requestId, level, requestedBy, req.getAction());

        if (req.isApprove()) {

            log.info(
                    "Approval accepted, moving to next step | requestId={} | level={}",
                    requestId,
                    level
            );

            handleNextLevelOrPostAction(
                    requestedBy,
                    requestId,
                    level,
                    approval.getWorkItemType()
            );

        } else {

            log.info(
                    "Approval rejected, cancelling future approvals | requestId={} | level={}",
                    requestId,
                    level
            );

            cancelFutureApprovals(requestId, level);
        }

        log.info(
                "END approveOrReject | approvalId={} | action={} | user={}",
                req.getApprovalId(),
                req.getAction(),
                loggedInUser
        );
    }


    private void makeParallelApprovals(
            String requestId,
            int level,
            String user,
            String action) {

        log.debug(
                "START makeParallelApprovals | requestId={} | level={} | action={}",
                requestId,
                level,
                action
        );

        List<ApprovalDetails> approvals =
                approvalDetailsRepository.findByRequestIdAndApprovalLevelAndApprovalStatus(
                        requestId,
                        level,
                        ApprovalStatus.Pending_Approval.name()
                );

        if (approvals.isEmpty()) {
            log.debug(
                    "No parallel approvals found | requestId={} | level={}",
                    requestId,
                    level
            );
            return;
        }

        approvals.forEach(a -> {
            a.setApprovalStatus("Not-Required");
            a.setApproverComment(action + " by " + user);
        });

        log.info(
                "Parallel approvals updated | requestId={} | updatedCount={}",
                requestId,
                approvals.size()
        );
    }


    private void handleNextLevelOrPostAction(
            String loggedInUser,
            String requestId,
            int level,
            String workItemType) {

        log.debug(
                "START handleNextLevelOrPostAction | requestId={} | level={} | workItemType={}",
                requestId,
                level,
                workItemType
        );

        List<ApprovalDetails> next =
                approvalDetailsRepository
                        .findByRequestIdAndApprovalStatusAndApprovalLevel(
                                requestId,
                                "Not-Started",
                                level + 1
                        );

        log.debug(
                "Next level approvals fetched | requestId={} | nextLevel={} | count={}",
                requestId,
                level + 1,
                next.size()
        );

        if (!next.isEmpty()) {
            next.forEach(a ->
                    a.setApprovalStatus(ApprovalStatus.Pending_Approval.name()));

            log.info(
                    "Next level approvals activated | requestId={} | level={}",
                    requestId,
                    level + 1
            );
        } else {
            log.info(
                    "No further approval levels, performing post-approval action | requestId={}",
                    requestId
            );
            performPostApprovalAction(loggedInUser, requestId, workItemType);
        }

        log.debug(
                "END handleNextLevelOrPostAction | requestId={}",
                requestId
        );
    }


    private void cancelFutureApprovals(String requestId, int level) {

        log.debug(
                "START cancelFutureApprovals | requestId={} | fromLevel={}",
                requestId,
                level
        );

        List<ApprovalDetails> future =
                approvalDetailsRepository.findByRequestIdAndApprovalStatusAndApprovalLevelGreaterThan(
                        requestId,
                        "Not-Started",
                        level
                );

        log.debug(
                "Future approvals found | requestId={} | count={}",
                requestId,
                future.size()
        );

        future.forEach(a -> {
            a.setApprovalStatus(ApprovalStatus.Cancelled.name());
            a.setApproverComment("Not-Required");
        });

        log.info(
                "Future approvals cancelled | requestId={} | cancelledCount={}",
                requestId,
                future.size()
        );
    }

    private void performPostApprovalAction(String loggedInUser, String requestId, String workItemType) {

        log.info(
                "START performPostApprovalAction | requestId={} | workItemType={} | user={}",
                requestId,
                workItemType,
                loggedInUser
        );

        if ("SERVER-ELEVATION".equalsIgnoreCase(workItemType)) {
            serverElevationService.performPostApprovalDenyActionServerElevation(loggedInUser, requestId);
            log.info(
                    "Post approval action executed for SERVER-ELEVATION | requestId={}",
                    requestId
            );
        }

        log.info(
                "END performPostApprovalAction | requestId={}",
                requestId
        );
    }

}
