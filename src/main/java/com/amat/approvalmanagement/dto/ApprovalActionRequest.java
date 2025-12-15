package com.amat.approvalmanagement.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ApprovalActionRequest {
    private UUID approvalId;
    private String action;
    private String comment;

    public boolean isApprove() {
        return "approve".equalsIgnoreCase(action);
    }

    public boolean isDeny() {
        return "deny".equalsIgnoreCase(action);
    }
}