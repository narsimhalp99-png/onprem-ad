package com.amat.approvalmanagement.service;

import com.amat.approvalmanagement.dto.ApprovalDetailsFilterDTO;
import com.amat.serverelevation.entity.ApprovalDetails;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Slf4j
public class ApprovalDetailsRequestSpecification {

    public static Specification<ApprovalDetails> applyFilters(ApprovalDetailsFilterDTO filter, String loggedInUser, boolean isSelf) {
        Specification<ApprovalDetails> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (isSelf) {
                predicates.add(cb.equal(root.get("requestorEmpId"), loggedInUser));
            }

            if (filter.getRequestId() != null && !filter.getRequestId().isEmpty()) {
                predicates.add(cb.equal(root.get("requestId"), filter.getRequestId()));
            }
            if (filter.getApprover() != null && !filter.getApprover().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("approver")), "%" + filter.getApprover().toLowerCase() + "%"));
            }
            if (filter.getApproverName() != null && !filter.getApproverName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("approverName")), "%" + filter.getApproverName().toLowerCase() + "%"));
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
        return spec;
    }

}
