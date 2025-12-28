package com.amat.approvalmanagement.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ReassignApprovalRequest {

    private String action;
    private UUID approvalId;
    private String comment;
    private String newApprover;
}
