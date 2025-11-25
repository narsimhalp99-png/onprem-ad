package com.amat.serverelevation.repository;



import com.amat.serverelevation.entity.ApprovalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalDetailsRepository extends JpaRepository<ApprovalDetails, String> {
}

