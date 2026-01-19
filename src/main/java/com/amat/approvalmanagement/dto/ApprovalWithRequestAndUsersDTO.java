package com.amat.approvalmanagement.dto;

import com.amat.accessmanagement.entity.UserEntity;
import com.amat.serverelevation.entity.ServerElevationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWithRequestAndUsersDTO {

    private UUID approvalId;
    private LocalDateTime approvalRequestDate;
    private String requestId;
    private String approver;
    private String workItemName;
    private String workItemType;
    private String approvalStatus;
    private String approverComment;
    private int approvalLevel;
    private LocalDateTime approvalDate;
    private String requestee;
    private String approverName;
    private String requestorEmpId;
    private ServerElevationRequest requestDetails;
    private UserEntity requestorDetails;
    private UserEntity approverDetails;
    private UserEntity oldApprover;

}

