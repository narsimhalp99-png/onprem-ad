package com.amat.serverelevation.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "approval_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDetails {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uniqueidentifier")
    private UUID approvalId;

    @Generated(GenerationTime.INSERT)
    @Column(
            nullable = false,
            updatable = false,
            insertable = false,
            columnDefinition = "DATETIME DEFAULT GETDATE()"
    )
    private LocalDateTime approvalRequestDate;

    @Column(nullable = false)
    private String requestId;

    @Column(nullable = false)
    private String approver;

    private String approverName;

    private String requestorEmpId;

    @Column(length = 500)
    private String workItemName;

    private String workItemType;

    private String approvalStatus;

    @Column(columnDefinition = "nvarchar(max)")
    private String approverComment;

}
