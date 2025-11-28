package com.amat.approvalmanagement.dto;


import lombok.Data;

@Data
public class ApprovalDetailsSearchDTO {
    private int page = 0;
    private int size = 10;
    private String requestId;
    ApprovalDetailsFilterDTO filter;
}
