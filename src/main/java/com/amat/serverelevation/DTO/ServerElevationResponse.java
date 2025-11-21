package com.amat.serverelevation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServerElevationResponse {
    private String serverName;
    private String operatingSystem;
    private String applicationName;
    private OwnerDetails ownerDetails;
    private Boolean eligibleForElevation;
    private String eligibleForElevationMsg;
    private Boolean approvalRequired;
}

