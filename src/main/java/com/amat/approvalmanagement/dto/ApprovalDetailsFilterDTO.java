package com.amat.approvalmanagement.dto;

import com.amat.approvalmanagement.enums.ApprovalStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalDetailsFilterDTO {

    private String requestId;
    private String approver;
    private String approvalStatus;
    private String workItemType;
    private String workItemName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime toDate;

    private String sortField;
    private String sortDirection;
}
