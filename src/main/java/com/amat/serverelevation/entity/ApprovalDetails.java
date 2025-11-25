package com.amat.serverelevation.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDetails {

    @Id
    @Column(insertable = false, updatable = false)
    private String approvalId;

    @Column(insertable = false, updatable = false)
    private LocalDateTime approvalRequestDate;

    @Column(nullable = false)
    private String requestId;

    @Column(nullable = false)
    private String approver;

    @Column(length = 500)
    private String workItemName;

    private String workItemType;

    private String approvalStatus;

    @Column(columnDefinition = "nvarchar(max)")
    private String approverComment;

}
