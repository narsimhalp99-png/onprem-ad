package com.amat.approvalmanagement.service;

import com.amat.accessmanagement.repository.UserPreferencesRepository;
import com.amat.accessmanagement.service.RoleService;
import com.amat.admanagement.service.GroupsService;
import com.amat.approvalmanagement.dto.ApprovalActionRequest;
import com.amat.approvalmanagement.dto.ApprovalDetailsFilterDTO;
import com.amat.approvalmanagement.dto.ApprovalWithRequestDTO;
import com.amat.approvalmanagement.dto.ReassignApprovalRequest;
import com.amat.approvalmanagement.enums.ApprovalStatus;
import com.amat.approvalmanagement.repository.ApprovalDetailsFilterRepository;
import com.amat.approvalmanagement.entity.ApprovalDetails;
import com.amat.approvalmanagement.repository.ApprovalDetailsRepository;
import com.amat.commonutils.entity.UserPreferences;
import com.amat.commonutils.utis.CommonUtils;
import com.amat.serverelevation.entity.ServerElevationRequest;
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
import java.util.stream.Collectors;

import org.springframework.web.server.ResponseStatusException;

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
    UserPreferencesRepository userPreferencesRepository;

    @Autowired
    CommonUtils commonUtils;

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

        Page<ApprovalDetails> pageData =
                approvalRepo.findAll(spec, pageable);

// Extract requestIds from approvals
        List<String> requestIds = pageData.getContent()
                .stream()
                .map(ApprovalDetails::getRequestId)
                .distinct()
                .toList();

//  Fetch request details in ONE query
        List<ServerElevationRequest> requests =
                serverElevationRequestRepository.findByRequestIdIn(requestIds);

//  Map requestId → ServerElevationRequest
        Map<String, ServerElevationRequest> requestMap =
                requests.stream()
                        .collect(Collectors.toMap(
                                ServerElevationRequest::getRequestId,
                                r -> r
                        ));

//  Map Approval → DTO
        Page<ApprovalWithRequestDTO> responsePage =
                pageData.map(approval -> ApprovalWithRequestDTO.builder()
                        .approvalId(approval.getApprovalId())
                        .approvalRequestDate(approval.getApprovalRequestDate())
                        .requestId(approval.getRequestId())
                        .approver(approval.getApprover())
                        .workItemName(approval.getWorkItemName())
                        .workItemType(approval.getWorkItemType())
                        .approvalStatus(approval.getApprovalStatus())
                        .approverComment(approval.getApproverComment())
                        .approvalLevel(approval.getApprovalLevel())
                        .approvalDate(approval.getApprovalDate())
                        .requestee(approval.getRequestee())
                        .requestDetails(
                                requestMap.get(approval.getRequestId())
                        )
                        .build()
                );
        log.info("END getApprovalDetails | user={} | resultReturned", loggedInUser);
        return ResponseEntity.ok(responsePage);

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
        String requestee = approval.getRequestee();
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

        log.debug(
                "Resolved request owner | requestId={} | requestee={}",
                requestId,
                requestee
        );

        // Handle parallel approvals
        makeParallelApprovals(requestId, level, requestee, req.getAction());

        if (req.isApprove()) {

            log.info(
                    "Approval accepted, moving to next step | requestId={} | level={}",
                    requestId,
                    level
            );

            handleNextLevelOrPostAction(
                    requestee,
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


        if("SERVER-ELEVATION".equalsIgnoreCase(approval.getWorkItemType())){
            serverRepo.updateOnFailure(
                    requestId,
                    null,
                    null,
                    "Completed"
            );
        }

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
            String requestee,
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
            performPostApprovalAction(requestee, requestId, workItemType);
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


    public void reassignApproval(ReassignApprovalRequest req, String loggedInUser) {

        log.info(
                "Starting approval reassignment | approvalId={} | initiatedBy={}",
                req.getApprovalId(),
                loggedInUser
        );

        // 1. Fetch existing approval
        ApprovalDetails existing = approvalRepo
                .findById(req.getApprovalId())
                .orElseThrow(() -> {
                    log.warn(
                            "Approval not found for reassignment | approvalId={} | initiatedBy={}",
                            req.getApprovalId(),
                            loggedInUser
                    );
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Approval not found"
                    );
                });
        if (!ApprovalStatus.Pending_Approval.name().equalsIgnoreCase(existing.getApprovalStatus())) {

            log.warn(
                    "Reassignment blocked | approvalId={} | currentStatus={} | initiatedBy={}",
                    existing.getApprovalId(),
                    existing.getApprovalStatus(),
                    loggedInUser
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Approval can be reassigned only when status is Pending_Approval"
            );
        }
        String oldApprover = existing.getApprover();

        log.info(
                "Existing approval fetched | approvalId={} | oldApprover={} | approvalLevel={}",
                existing.getApprovalId(),
                oldApprover,
                existing.getApprovalLevel()
        );

        // 2. Update existing approval entry
        existing.setApprovalStatus(ApprovalStatus.ReAssigned.name());
        existing.setApproverComment(
                String.format(
                        "Reassigned from '%s' to '%s' by %s. Comment: %s",
                        oldApprover,
                        req.getNewApprover(),
                        loggedInUser,
                        req.getComment()
                )
        );
//        existing.setApprovalDate(LocalDateTime.now());

        approvalRepo.save(existing);

        log.info(
                "Existing approval marked as ReAssigned | approvalId={} | oldApprover={} | newApprover={}",
                existing.getApprovalId(),
                oldApprover,
                req.getNewApprover()
        );


        String approverEmpId = req.getNewApprover();
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


        // 3. Create new approval entry
        ApprovalDetails newApproval = ApprovalDetails.builder()
                .requestId(existing.getRequestId().toUpperCase())
                .approver(finalApprover)
                .workItemName(existing.getWorkItemName())
                .workItemType(existing.getWorkItemType())
                .approvalLevel(existing.getApprovalLevel())
                .approvalStatus(ApprovalStatus.Pending_Approval.name())
                .requestee(existing.getRequestee())
                .requestor(existing.getRequestor())
                .approvalRequestDate(LocalDateTime.now())
                .build();

        approvalRepo.save(newApproval);

        log.info(
                "New approval created | newApprovalId={} | requestId={} | approver={} | status={}",
                newApproval.getApprovalId(),
                newApproval.getRequestId(),
                newApproval.getApprover(),
                newApproval.getApprovalStatus()
        );

        // 4. Update server_elevation_requests.approver_id
        int updatedRows = 0;
        if ("SERVER-ELEVATION".equalsIgnoreCase(newApproval.getWorkItemType())) {

            log.info(
                    "SERVER-ELEVATION detected. Updating approvalId in server_elevation_requests. " +
                            "requestId={}, newApprovalId={}",
                    newApproval.getRequestId(),
                    newApproval.getApprovalId()
            );

            try {
                updatedRows = serverRepo.updateApprovalId(
                        newApproval.getRequestId(),
                        String.valueOf(newApproval.getApprovalId()).toUpperCase()
                );

                if (updatedRows > 0) {
                    log.info(
                            "Successfully updated server_elevation_requests with new approvalId. " +
                                    "requestId={}, approvalId={}, rowsUpdated={}",
                            newApproval.getRequestId(),
                            newApproval.getApprovalId(),
                            updatedRows
                    );
                } else {
                    log.warn(
                            "No rows updated in server_elevation_requests. Possible data issue. " +
                                    "requestId={}, approvalId={}",
                            newApproval.getRequestId(),
                            newApproval.getApprovalId()
                    );
                }

            } catch (Exception ex) {
                log.error(
                        "Failed to update approvalId in server_elevation_requests. " +
                                "requestId={}, approvalId={}",
                        newApproval.getRequestId(),
                        newApproval.getApprovalId(),
                        ex
                );
                throw ex; // or wrap in ResponseStatusException if needed
            }
        }



        if (updatedRows == 0) {
            log.warn("No {} requests row updated for requestId={}",newApproval.getWorkItemType(), newApproval.getRequestId());
        } else {
            log.info(" {} requests updated with new approvalId. requestId={}, newApprovalId={}",newApproval.getWorkItemType(),
                    newApproval.getRequestId(), newApproval.getApprovalId());
        }

        log.info("Approval reassignment completed successfully. requestId={}", newApproval.getRequestId());

    }

}
