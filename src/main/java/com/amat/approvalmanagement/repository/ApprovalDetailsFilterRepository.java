package com.amat.approvalmanagement.repository;

import com.amat.serverelevation.entity.ApprovalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ApprovalDetailsFilterRepository extends
        JpaRepository<ApprovalDetails, UUID>,
        JpaSpecificationExecutor<ApprovalDetails> {
}
