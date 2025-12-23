package com.amat.approvalmanagement.controller;

import com.amat.approvalmanagement.dto.ApiResponse;
import com.amat.approvalmanagement.dto.ApprovalActionRequest;
import com.amat.approvalmanagement.dto.ApprovalDetailsSearchDTO;
import com.amat.approvalmanagement.service.ApprovalsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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
    public ResponseEntity<ApiResponse> approveOrReject(
            @RequestBody ApprovalActionRequest request,
            HttpServletRequest httpServletRequest) {

        log.info("API HIT: /approval-management/approveOrReject");

        String loggedInUser = httpServletRequest.getHeader("employeeId");
        log.debug("Logged-in user extracted from header | employeeId={}", loggedInUser);

        log.info(
                "Processing approval action | action={} | requestId={} | user={}",
                request.getAction(),
                request.getApprovalId(),
                loggedInUser
        );

        approvalService.approveOrReject(request, loggedInUser);

        log.info(
                "Approval action processed successfully | action={} | requestId={} | user={}",
                request.getAction(),
                request.getApprovalId(),
                loggedInUser
        );

        return ResponseEntity.ok(
                new ApiResponse("SUCCESS", "Action processed successfully")
        );
    }

}
