package com.amat.serverelevation.DTO;


import lombok.Data;

import java.util.List;

@Data
public class SubmitElevationRequest {

    private String comment;
    private List<SubmitServerEntry> eligibleServers;
}
