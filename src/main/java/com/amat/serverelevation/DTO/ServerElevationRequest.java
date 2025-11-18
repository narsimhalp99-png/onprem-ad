package com.amat.serverelevation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServerElevationRequest {
    private String computerName;
    private String requestorEmpId;
}

