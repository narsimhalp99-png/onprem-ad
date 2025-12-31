package com.amat.serverelevation.DTO;

import com.amat.approvalmanagement.entity.ApprovalDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServerElevationRequestResponse {

    private Integer id;
    private String requestId;
    private String requestedBy;
    private String serverName;
    private Integer durationInHours;
    private String requestorComment;
    private LocalDateTime requestDate;
    private LocalDateTime elevationTime;
    private String elevationStatus;
    private LocalDateTime deElevationTime;
    private String deElevationStatus;
    private String elevationStatusMessage;
    private String deElevationStatusMessage;
    private String status;
    private String approvalId;

    private List<ApprovalDetails> approvalDetails;
}
