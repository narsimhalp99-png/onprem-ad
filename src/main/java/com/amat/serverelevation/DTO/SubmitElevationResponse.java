package com.amat.serverelevation.DTO;


import lombok.Data;

import java.util.List;

@Data
public class SubmitElevationResponse {
    private String requestorEmpId;
    private String comments;
    private List<EligibleServer> eligibleServers;

    @Data
    public static class EligibleServer {
        private String serverName;
        private String duration;
    }
}
