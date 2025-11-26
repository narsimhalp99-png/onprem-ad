package com.amat.serverelevation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerElevationRequestFilterDTO {
    private String serverName;
    private String status;
    private String elevationStatus;
    private String deElevationStatus;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}

