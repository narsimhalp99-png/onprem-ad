package com.amat.approvalmanagement.controller;


import com.amat.approvalmanagement.dto.ApiResponse;
import com.amat.approvalmanagement.dto.ApprovalActionRequest;
import com.amat.approvalmanagement.dto.ApprovalDetailsSearchDTO;
import com.amat.approvalmanagement.service.ApprovalsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/approval-management")
public class ApprovalsController {


    @Autowired
    ApprovalsService approvalService;

    @PostMapping("/get-approval-details")
    public Object getApprovalDetails(@RequestBody ApprovalDetailsSearchDTO request, HttpServletRequest req) {

        String loggedInUser = req.getHeader("employeeId");
        boolean isSelf = request.getAssignedTo().equalsIgnoreCase("self");

        return approvalService.getApprovalDetails(
                request.getFilter(),
                request.getPage(),
                request.getSize(),
                loggedInUser,
                isSelf
        );
    }


    @PostMapping("/approveOrReject")
    public ResponseEntity<ApiResponse> approveOrReject(
            @RequestBody ApprovalActionRequest request,
            HttpServletRequest httpServletRequest) {

        String loggedInUser = httpServletRequest.getHeader("employeeId");
        approvalService.approveOrReject(request,loggedInUser);

        return ResponseEntity.ok(
                new ApiResponse("SUCCESS", "Action processed successfully")
        );
    }

}
