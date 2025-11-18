package com.amat.serverelevation.service;


import com.amat.serverelevation.DTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerElevationService {

    @Autowired
    LdapService ldapService;

    public ServerElevationResponse validateRequest(ServerElevationRequest request, boolean additionalDetails) {
        ServerElevationResponse response = ServerElevationResponse.builder()
                .computerName(request.getComputerName())
                .build();

        // Step 1: Find Active AD Computer
        AdComputer computer = ldapService.findActiveComputer(request.getComputerName())
                .orElseGet(() -> {
                    setError(response, "SERVER_NOT_FOUND");
                    return null;
                });

        if (computer == null) return response;

        response.setOperatingSystem(computer.getOperatingSystem());
        response.setApplicationName(getApplicationNameFromComputer(computer));

        // Step 2 & 3: Get Server Admin Group
        String adminGroup = request.getComputerName() + "-APP-ADMINS";
        AdGroup group = ldapService.findGroupWithManagedBy(adminGroup)
                .orElseGet(() -> {
                    setError(response, "SERVER_OWNER_NOT_FOUND");
                    return null;
                });

        if (group == null) return response;

        // Step 4: Get Owner Details
        AdUser owner = ldapService.getUserByDn(group.getManagedBy())
                .orElseGet(() -> {
                    setError(response, "OWNER_NOT_FOUND");
                    return null;
                });

        if (owner == null) return response;

        response.setOwnerDetails(OwnerDetails.builder()
                .ownerEmpID(owner.getEmployeeId())
                .ownerName(owner.getDisplayName())
                .build());

        // Step 5: Check if User Already Elevated
        String localAdminGroup = request.getComputerName() + "-Local-Admins";
        if (ldapService.isUserInGroup(request.getRequestorEmpId(), localAdminGroup)) {
            setError(response, "USER_ALREADY_ELEVATED");
            return response;
        }

        response.setEligibleForElevation(true);

        // Step 6: Check if Approval Required (only if additionalDetails is true)
        if (additionalDetails) {
            boolean isMemberOfAdminGroup = ldapService.isUserInGroup(request.getRequestorEmpId(), adminGroup);
            response.setApprovalRequired(!isMemberOfAdminGroup);
        }

        return response;
    }

    private void setError(ServerElevationResponse response, String errorMsg) {
        response.setEligibleForElevation(false);
        response.setEligibleForElevationMsg(errorMsg);
    }

    private String getApplicationNameFromComputer(AdComputer computer) {
        return "MyID-IIQ";
    }
}

