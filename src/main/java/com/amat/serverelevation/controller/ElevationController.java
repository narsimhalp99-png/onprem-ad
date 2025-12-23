package com.amat.serverelevation.controller;

import com.amat.serverelevation.DTO.getServerElevationRequests;
import com.amat.serverelevation.DTO.*;
import com.amat.serverelevation.entity.ServerElevationRequest;
import com.amat.serverelevation.service.ServerElevationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/server-elevation")
@Slf4j
public class ElevationController {

    @Autowired
    ServerElevationService serverElevationService;

    @GetMapping("/fetchServerDetails")
    public ResponseEntity<ServerElevationResponse> validateServerElevation(
            @RequestParam(name = "serverName", defaultValue = "") String serverName,
            HttpServletRequest servletRequest) {

        log.info("API HIT: /server-elevation/fetchServerDetails | serverName={}", serverName);

        String employeeId = servletRequest.getHeader("employeeId");
        log.debug("EmployeeId extracted from header | employeeId={}", employeeId);

        ServerElevationRequestDTO request = new ServerElevationRequestDTO();
        request.setServerName(serverName);
        request.setRequestorEmpId(employeeId);

        log.debug(
                "ServerElevationRequestDTO constructed | serverName={} | requestorEmpId={}",
                serverName,
                employeeId
        );

        ServerElevationResponse response = new ServerElevationResponse();

        if (serverName == null || serverName.isBlank()) {
            log.warn("Validation failed: serverName is null or blank");
            response.setEligibleForElevation(false);
            response.setEligibleForElevationMsg("Server Name Cannot be blank or null");
        } else if (employeeId == null || employeeId.isBlank()) {
            log.warn("Validation failed: employeeId is null or blank");
            response.setEligibleForElevation(false);
            response.setEligibleForElevationMsg("EmployeeId Cannot be blank or null");
        } else {
            log.info(
                    "Invoking server elevation validation | serverName={} | employeeId={}",
                    serverName,
                    employeeId
            );
            response = serverElevationService.validateRequest(request);
            log.info(
                    "Server elevation validation completed | serverName={} | eligible={}",
                    serverName,
                    response.getEligibleForElevation()
            );
        }

        log.info("END /server-elevation/fetchServerDetails | serverName={}", serverName);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit-requests")
    public ResponseEntity<Object> submitRequests(
            @RequestBody SubmitElevationRequest request,
            HttpServletRequest servletRequest) {

        log.info("API HIT: /server-elevation/submit-requests | request={}", request);

        String employeeId = servletRequest.getHeader("employeeId");
        log.debug("EmployeeId extracted from header | employeeId={}", employeeId);

        log.info(
                "Submitting server elevation request | employeeId={} | serverName={}",
                employeeId,
                request.getEligibleServers()
        );

        serverElevationService.submitElevationRequest(employeeId, request);

        log.info(
                "Server elevation request submitted successfully | employeeId={} | serverName={}",
                employeeId,
                request.getEligibleServers()
        );

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                "Request submitted successfully and being processed"
        ));
    }

    @PostMapping("/get-requests")
    public Object getRequests(
            @RequestBody getServerElevationRequests serverEleReq,
            HttpServletRequest req) {

        log.info("API HIT: /server-elevation/get-requests | request={}", serverEleReq);

        String loggedInUser = req.getHeader("employeeId");
        log.debug("Logged-in user extracted from header | employeeId={}", loggedInUser);

        boolean isSelf = serverEleReq.getRequestedBy().equalsIgnoreCase("self");
        log.debug(
                "Computed isSelf flag | requestedBy={} | isSelf={}",
                serverEleReq.getRequestedBy(),
                isSelf
        );

        String validSortField = (serverEleReq.getFilter().getSortField() != null
                && !serverEleReq.getFilter().getSortField().isEmpty())
                ? serverEleReq.getFilter().getSortField()
                : "requestDate";

        Sort.Direction direction =
                (serverEleReq.getFilter().getSortDirection() != null
                        && serverEleReq.getFilter().getSortDirection().equalsIgnoreCase("desc"))
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        log.debug(
                "Pagination & sorting resolved | sortField={} | direction={} | page={} | size={}",
                validSortField,
                direction,
                serverEleReq.getPage(),
                serverEleReq.getSize()
        );

        Pageable pageable = PageRequest.of(
                serverEleReq.getPage(),
                serverEleReq.getSize(),
                Sort.by(direction, validSortField)
        );

        log.info(
                "Fetching server elevation requests | user={} | isSelf={}",
                loggedInUser,
                isSelf
        );

        Object response =
                serverElevationService.getRequests(serverEleReq, loggedInUser, isSelf, pageable);

        log.info(
                "END /server-elevation/get-requests | user={}",
                loggedInUser
        );

        return response;
    }

}
