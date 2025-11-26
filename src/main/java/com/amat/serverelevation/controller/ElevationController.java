package com.amat.serverelevation.controller;


import com.amat.admanagement.service.UserService;
import com.amat.serverelevation.DTO.*;
import com.amat.serverelevation.service.ServerElevationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/server-elevation")
public class ElevationController {

    @Autowired
    ServerElevationService serverElevationService;

    @GetMapping("/fetchServerDetails")
    public ResponseEntity<ServerElevationResponse> validateServerElevation( @RequestParam(name = "serverName", defaultValue = "") String serverName, HttpServletRequest servletRequest) {

        String employeeId = servletRequest.getHeader("employeeId");
        ServerElevationRequest request = new ServerElevationRequest();
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
    public ResponseEntity<List<SubmitResponse>> submitRequests(@RequestBody SubmitElevationRequest request, HttpServletRequest servletRequest) {
        String employeeId = servletRequest.getHeader("employeeId");

        List<SubmitResponse> result = serverElevationService.submitElevationRequest(employeeId, request);
        return ResponseEntity.ok(result);
    }



}
