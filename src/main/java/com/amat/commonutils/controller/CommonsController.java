package com.amat.commonutils.controller;

import com.amat.commonutils.dto.SystemConfigUpdateRequest;
import com.amat.commonutils.dto.UserPreferencesRequest;
import com.amat.accessmanagement.dto.UserSearchResponseDTO;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.service.SearchUsersService;
import com.amat.accessmanagement.service.UserEnrollmentService;
import com.amat.commonutils.entity.SystemConfigurations;
import com.amat.commonutils.repository.AuditRepository;
import com.amat.commonutils.service.AuditService;
import com.amat.commonutils.service.SystemConfigurationsService;
import com.amat.commonutils.service.UserPreferencesService;
import com.amat.commonutils.util.CommonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/common-utils")
public class CommonsController {

    @Autowired
    UserEnrollmentService svc;

    @Autowired
    SearchUsersService searchUsersService;

    @Autowired
    UserPreferencesService userPreferencesService;

    @Autowired
    SystemConfigurationsService sysConfigSvc;

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    AuditService auditService;

    @GetMapping("/getLoggedInUserDetails")
    public ResponseEntity<Object> getUser(
            @RequestParam(name = "additionalDetails", defaultValue = "false") boolean additionalDetails,
            HttpServletRequest servletRequest) {

        log.info(
                "API HIT: getLoggedInUserDetails | additionalDetails={}",
                additionalDetails
        );

        String employeeId = servletRequest.getHeader("employeeId");

        log.debug("EmployeeId extracted from header | employeeId={}", employeeId);

        UserEntity user = svc.getUser(employeeId, additionalDetails);

        if (user == null) {

            log.warn(
                    "Logged-in user not found | employeeId={}",
                    employeeId
            );

            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", "failed",
                            "message", "User not found: " + employeeId
                    )
            );
        }

        log.info(
                "Logged-in user details fetched successfully | employeeId={}",
                employeeId
        );

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "data", user
                )
        );
    }

    @GetMapping("/searchUsers")
    public Page<UserSearchResponseDTO> getUsers(
            @RequestParam(required = false) String searchString,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        log.info(
                "ENTER /searchUsers | searchString={} | page={} | size={}",
                searchString,
                page,
                size
        );

        Page<UserSearchResponseDTO> response =
                searchUsersService.searchUsers(searchString, page, size);

        log.info(
                "EXIT /searchUsers | totalElements={} | totalPages={} | returnedSize={}",
                response.getTotalElements(),
                response.getTotalPages(),
                response.getNumberOfElements()
        );

        return response;
    }

    @PostMapping("/user-preferences")
    public ResponseEntity<Map<String, String>> updatePreferences(
            @RequestBody UserPreferencesRequest req,
            HttpServletRequest request
    ) {
        String employeeId = request.getHeader("employeeId");


        if(req.getOperation().equalsIgnoreCase("favtiles")){

            log.info(
                    "Received create/update preferences request for employeeId={}, AddFavTiles={},  RemoveFavTiles={}",
                    employeeId,
                    req.getAddFavTiles(), req.getRemoveFavTiles()
            );

            userPreferencesService.createOrUpdatePreferences(
                    employeeId,
                    req.getAddFavTiles(),
                    req.getRemoveFavTiles()
            );
        }else if (req.getOperation().equalsIgnoreCase("ooo")){
            log.info(
                    "Received OOO update request | employeeId={} | enabled={} | start={} | end={} | approver={}",
                    employeeId,
                    req.isOooEnabled(),
                    req.getOooStartDate(),
                    req.getOooEndDate(),
                    req.getOooApprover()
            );

            userPreferencesService.updateOOODetails(employeeId,req);
        }



        return ResponseEntity.ok(Map.of(
                "message", "Preferences updated successfully"
        ));
    }

    @GetMapping("/user-preferences")
    public Object getPreferences(
            HttpServletRequest request
    ) {
        String employeeId = request.getHeader("employeeId");

        log.info("Received get preferences request for employeeId={}", employeeId);

        return ResponseEntity.ok(
                userPreferencesService.getPreferences(employeeId)
        );
    }


    @GetMapping("/system-configurations")
    public ResponseEntity<List<SystemConfigurations>> getAllConfigs() {
        auditService.auditAdOperation(
                "SERVER-ELEVATION",
                "loggedInUser",
                "ACTIVE-DIRECTORY",
                "MEMBER_ADD",
                "groupDn",
                null,
                "useradminDN",
                "RequestId: " + "123423424"
        );

        return ResponseEntity.ok(sysConfigSvc.getAllConfigs());
    }

    @GetMapping("/system-configurations/group")
    public ResponseEntity<?> getAllGroupConfigs() {
        return ResponseEntity.ok(commonUtils.loadSystemConfigurations());
    }

    @GetMapping("/system-configurations/{configType}")
    public ResponseEntity<List<SystemConfigurations>> getConfigsByType(
            @PathVariable String configType) {
        return ResponseEntity.ok(sysConfigSvc.getConfigsByType(configType));
    }

    @PutMapping("/system-configurations")
    public ResponseEntity<SystemConfigurations> updateConfig(
            @RequestBody SystemConfigUpdateRequest request) {

        return ResponseEntity.ok(sysConfigSvc.updateConfigValue(request));
    }

}
