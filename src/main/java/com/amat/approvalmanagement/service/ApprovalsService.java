package com.amat.approvalmanagement.service;



import com.amat.approvalmanagement.dto.ApprovalDetailsFilterDTO;
import com.amat.approvalmanagement.dto.ApprovalDetailsSearchDTO;
import com.amat.approvalmanagement.repository.ApprovalDetailsFilterRepository;
import com.amat.serverelevation.entity.ApprovalDetails;
import com.amat.serverelevation.repository.ApprovalDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Slf4j
@Service
public class ApprovalsService {


    @Autowired
    ApprovalDetailsFilterRepository approvalRepo;


    public Page<ApprovalDetails> getApprovalDetails(ApprovalDetailsSearchDTO request, ApprovalDetailsFilterDTO filter, int page, int size) {


        String validSortField = (filter.getSortField() != null && !filter.getSortField() .isEmpty())
                ? filter.getSortField()
                : "approvalRequestDate";

        Sort.Direction direction = (filter.getSortDirection()  != null && filter.getSortDirection().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortField));

//        Pageable pageable = PageRequest.of(page, size, Sort.by("approvalRequestDate").ascending());

        Specification<ApprovalDetails> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (request.getRequestId() != null && !request.getRequestId().isEmpty()) {
                predicates.add(cb.equal(root.get("requestId"), request.getRequestId()));
            }
            if (filter.getApprover() != null && !filter.getApprover().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("approver")), "%" + filter.getApprover().toLowerCase() + "%"));
            }
            if (filter.getApprovalStatus() != null && !filter.getApprovalStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("approvalStatus"), filter.getApprovalStatus()));
            }
            if (filter.getWorkItemType() != null && !filter.getWorkItemType().isEmpty()) {
                predicates.add(cb.equal(root.get("workItemType"), filter.getWorkItemType()));
            }
            if (filter.getWorkItemName() != null && !filter.getWorkItemName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("workItemName")), "%" + filter.getWorkItemName().toLowerCase() + "%"));
            }

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("approvalRequestDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("approvalRequestDate"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return approvalRepo.findAll(spec, pageable);
    }


}
