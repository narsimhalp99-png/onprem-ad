package com.amat.approvalmanagement.controller;


import com.amat.approvalmanagement.dto.ApprovalDetailsSearchDTO;
import com.amat.approvalmanagement.service.ApprovalsService;
import com.amat.serverelevation.entity.ApprovalDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.method.AuthorizeReturnObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
