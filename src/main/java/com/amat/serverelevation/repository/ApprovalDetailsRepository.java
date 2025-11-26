package com.amat.serverelevation.repository;



import com.amat.serverelevation.entity.ApprovalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalDetailsRepository extends JpaRepository<ApprovalDetails, String> {

    List<ApprovalDetails> findByRequestIdInOrderByApprovalRequestDateDesc(List<String> requestIds);
}

