package com.amat.approvalmanagement.controller;


import com.amat.approvalmanagement.dto.ApprovalDetailsSearchDTO;
import com.amat.approvalmanagement.service.ApprovalsService;
import com.amat.serverelevation.entity.ApprovalDetails;
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
    public ResponseEntity<?> getApprovalDetails(@RequestBody ApprovalDetailsSearchDTO request) {

        Page<ApprovalDetails> result = approvalService.getApprovalDetails(request,
                request.getFilter(),
                request.getPage(),
                request.getSize()
        );

        return ResponseEntity.ok(result);
    }

}
