package com.amat.serverelevation.controller;


import com.amat.approverequests.dto.getApprovalsDTO;
import com.amat.serverelevation.DTO.*;
import com.amat.serverelevation.entity.ServerElevationRequest;
import com.amat.serverelevation.service.ServerElevationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;


@RestController
@RequestMapping("/server-elevation")
public class ElevationController {

    @Autowired
    ServerElevationService serverElevationService;

    @GetMapping("/fetchServerDetails")
    public ResponseEntity<ServerElevationResponse> validateServerElevation( @RequestParam(name = "serverName", defaultValue = "") String serverName, HttpServletRequest servletRequest) {

        String employeeId = servletRequest.getHeader("employeeId");
        ServerElevationRequestDTO request = new ServerElevationRequestDTO();
        request.setServerName(serverName);
        request.setRequestorEmpId(employeeId);

        ServerElevationResponse response = new ServerElevationResponse();

        if (serverName==null ||  serverName.isBlank()) {
            response.setEligibleForElevation(false);
                    response.setEligibleForElevationMsg("Server Name Cannot be blank or null");
        }else if (employeeId==null ||  employeeId.isBlank()) {
            response.setEligibleForElevation(false);
            response.setEligibleForElevationMsg("EmployeeId Cannot be blank or null");
        }else{
            response = serverElevationService.validateRequest(request);
        }

        return ResponseEntity.ok(response);
    }


    @PostMapping("/submit-requests")
    public ResponseEntity<Object> submitRequests(@RequestBody SubmitElevationRequest request, HttpServletRequest servletRequest) {

        String employeeId = servletRequest.getHeader("employeeId");
        serverElevationService.submitElevationRequest(employeeId, request);
        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                "Request submitted successfully and being processed"
        ));
    }



}
