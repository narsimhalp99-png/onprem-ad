package com.amat.serverelevation.controller;


import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.UserService;
import com.amat.serverelevation.DTO.ServerElevationRequest;
import com.amat.serverelevation.DTO.ServerElevationResponse;
import com.amat.serverelevation.DTO.SubmitElevationRequest;
import com.amat.serverelevation.DTO.SubmitElevationResponse;
import com.amat.serverelevation.service.ServerElevationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/server-elevation")
public class ElevationController {

    @Autowired
    ServerElevationService serverElevationService;

    @Autowired
    UserService service;

    @PostMapping("/validateRequest")
    public ResponseEntity<ServerElevationResponse> validateServerElevation(
            @RequestParam(name = "additionalDetails", defaultValue = "false") boolean additionalDetails,
            @RequestBody ServerElevationRequest request) {

        ServerElevationResponse response = serverElevationService.validateRequest(request, additionalDetails);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submitElevationRequest")
    public ResponseEntity<SubmitElevationResponse> submitElevationRequest(
            @RequestBody SubmitElevationRequest request) {

        SubmitElevationResponse response = serverElevationService.submitElevationRequest(request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/admin-account/{employeeId}")
    public Map<String, Object> getAdminAccountDetails(@PathVariable Long employeeId) {
        return serverElevationService.fetchAdminAccountDetails(employeeId);
    }



}
