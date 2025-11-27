package com.amat.approverequests.controller;

import com.amat.approverequests.dto.getApprovalsDTO;
import com.amat.approverequests.service.ApprovalsService;
import com.amat.serverelevation.entity.ServerElevationRequest;
import com.amat.serverelevation.service.ServerElevationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/approve-requests")
public class ApprovalsController {


    @Autowired
    ApprovalsService approvalsService;


    @PostMapping("/get-approvals")
    public ResponseEntity<?> getRequests(
            @RequestBody getApprovalsDTO approvalsDTO,
            HttpServletRequest req) {

        String loggedInUser = req.getHeader("employeeId");

        boolean isSelf = approvalsDTO.getRequestedBy().equalsIgnoreCase("self");

        Pageable pageable = PageRequest.of(approvalsDTO.getPage(), approvalsDTO.getSize());

        Page<ServerElevationRequest> response =
                approvalsService.getRequests(approvalsDTO, loggedInUser, isSelf, pageable);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Requests fetched successfully",
                        "page", response
                )
        );
    }


}
