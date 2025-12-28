package com.amat.serverelevation.DTO;

import lombok.Data;

@Data
public class CancelServerElevationRequest {
    private String requestId;
    private String comment;
}
