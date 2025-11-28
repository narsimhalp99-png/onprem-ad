package com.amat.serverelevation.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ServerRequestsFilter {

    private String requestedBy;
    private String requestorName;
    private String serverName;
    private String status;
    private String elevationStatus;
    private String deElevationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime toDate;
}
