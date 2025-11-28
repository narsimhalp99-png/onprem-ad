package com.amat.serverelevation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class getServerElevationRequests {
    private String requestedBy;
    private int page;
    private int size;
    private ServerRequestsFilter filter;

}

