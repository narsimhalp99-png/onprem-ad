package com.amat.approvalmanagement.repository;



import com.amat.approvalmanagement.entity.ApprovalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalDetailsRepository extends JpaRepository<ApprovalDetails, String> {

    List<ApprovalDetails> findByRequestIdInOrderByApprovalRequestDateDesc(List<String> requestIds);


    Optional<ApprovalDetails> findByApprovalIdAndApproverAndApprovalStatus(
            UUID approvalId,
            String approver,
            String status
    );


    List<ApprovalDetails> findByRequestIdAndApprovalLevelAndApprovalStatus(
            String requestId,
            int approvalLevel,
            String status
    );

    List<ApprovalDetails>
    findByRequestIdAndApprovalStatusAndApprovalLevelGreaterThan(
            String requestId,
            String status,
            int approvalLevel
    );


    List<ApprovalDetails>
    findByRequestIdAndApprovalStatusAndApprovalLevel(
            String requestId,
            String status,
            int approvalLevel
    );

    @Modifying
    @Query("""
    UPDATE ApprovalDetails a
    SET a.approvalStatus = 'Cancelled',
        a.approverComment = :comment
    WHERE a.requestId = :requestId
      AND a.approvalStatus = 'Pending_Approval'
""")
    int cancelApprovals(
            @Param("requestId") String requestId,
            @Param("comment") String comment
    );





}

