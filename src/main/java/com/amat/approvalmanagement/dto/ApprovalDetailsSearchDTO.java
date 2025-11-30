package com.amat.approvalmanagement.dto;


import lombok.Data;

@Data
public class ApprovalDetailsSearchDTO {
    private String requestedBy;
    private int page = 0;
    private int size = 10;
    ApprovalDetailsFilterDTO filter;
}
