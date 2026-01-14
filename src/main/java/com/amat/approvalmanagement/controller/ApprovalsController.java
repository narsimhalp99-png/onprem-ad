package com.amat.approvalmanagement.controller;

import com.amat.approvalmanagement.dto.*;
import com.amat.approvalmanagement.service.ApprovalsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/approval-management")
@Slf4j
public class ApprovalsController {

    @Autowired
    ApprovalsService approvalService;

    @PostMapping("/get-approval-details")
    public Object getApprovalDetails(@RequestBody ApprovalDetailsSearchDTO request, HttpServletRequest req) {

        log.info("API HIT: /approval-management/get-approval-details");

        String loggedInUser = req.getHeader("employeeId");
        log.debug("Logged-in user extracted from header | employeeId={}", loggedInUser);

        boolean isSelf = request.getAssignedTo().equalsIgnoreCase("self");
        log.debug("Computed isSelf flag | assignedTo={} | isSelf={}", request.getAssignedTo(), isSelf);

        log.info(
                "Fetching approval details | user={} | page={} | size={} | isSelf={}",
                loggedInUser,
                request.getPage(),
                request.getSize(),
                isSelf
        );

        Object response = approvalService.getApprovalDetails(
                request.getFilter(),
                request.getPage(),
                request.getSize(),
                loggedInUser,
                isSelf
        );

        log.info("Approval details fetched successfully | user={}", loggedInUser);

        return response;
    }

    @PostMapping("/approveOrReject")
    public ResponseEntity<?> approveOrReject(
            @RequestBody ApprovalActionRequest request,
            HttpServletRequest httpServletRequest) {

        log.info("API HIT: POST /approval-management/approveOrReject");

        String loggedInUser = httpServletRequest.getHeader("employeeId");
        log.debug("Logged-in user extracted from token header | employeeId={}", loggedInUser);

        if (loggedInUser == null || loggedInUser.isBlank()) {
            log.warn("Missing employeeId header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("FAILED", "Missing LoggedIn user info"));
        }

        log.info(
                "Processing approval action | approvalId={} | action={} | user={}",
                request.getApprovalId(),
                request.getAction(),
                loggedInUser
        );

        try {
            approvalService.approveOrReject(request, loggedInUser);

            log.info(
                    "Approval action completed successfully | approvalId={} | action={}",
                    request.getApprovalId(),
                    request.getAction()
            );

            return ResponseEntity.ok(
                    new ApiResponse("SUCCESS", "Action processed successfully")
            );

        } catch (AccessDeniedException ex) {

            log.warn(
                    "Access denied during approveOrReject | approvalId={} | user={}",
                    request.getApprovalId(),
                    loggedInUser
            );

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("FAILED", ex.getMessage()));

        } catch (IllegalStateException ex) {

            log.warn(
                    "Invalid approval state | approvalId={} | reason={}",
                    request.getApprovalId(),
                    ex.getMessage()
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("FAILED", ex.getMessage()));

        } catch (Exception ex) {

            log.error(
                    "Unexpected error during approveOrReject | approvalId={}",
                    request.getApprovalId(),
                    ex
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("FAILED", "Internal server error"));
        }
    }


    @PostMapping("/reassign")
    public ResponseEntity<?> reassignApproval(
            @RequestBody ReassignApprovalRequest req,
            HttpServletRequest httpServletRequest
    ) {

        String loggedInUser = httpServletRequest.getHeader("employeeId");
        log.info(
                "Reassign approval request received | approvalId={} | newApprover={} | requestedBy={}",
                req.getApprovalId(),
                req.getNewApprover(),
                loggedInUser
        );

        try {
            approvalService.reassignApproval(req, loggedInUser);

            log.info(
                    "Approval reassigned successfully | approvalId={} | to newApprover={}  | by={}",
                    req.getApprovalId(),
                    req.getNewApprover(),
                    loggedInUser
            );

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Approval reassigned successfully"
            ));

        } catch (Exception ex) {

            log.error(
                    "Failed to reassign approval | approvalId={} | newApprover={} | by={}",
                    req.getApprovalId(),
                    req.getNewApprover(),
                    loggedInUser,
                    ex
            );

            throw ex;
        }
    }


    @PostMapping("/get-approval-details-by-id")
    public ApprovalWithRequestAndUsersDTO getApprovalDetails( HttpServletRequest req) {

        String loggedInUser = req.getHeader("employeeId");

        return approvalService.getApprovalById(UUID.fromString(loggedInUser));


    }

}
