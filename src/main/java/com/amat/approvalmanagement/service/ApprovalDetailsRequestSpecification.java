package com.amat.approvalmanagement.service;

import com.amat.approvalmanagement.dto.ApprovalDetailsFilterDTO;
import com.amat.approvalmanagement.entity.ApprovalDetails;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Slf4j
public class ApprovalDetailsRequestSpecification {

    public static Specification<ApprovalDetails> applyFilters(ApprovalDetailsFilterDTO filter, String loggedInUser, boolean isSelf) {

        log.info(
                "START applyFilters | user={} | isSelf={} | filter={}",
                loggedInUser,
                isSelf,
                filter
        );

        Specification<ApprovalDetails> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (isSelf) {
                log.debug("Applying self-approval filter | approver={}", loggedInUser);
                predicates.add(cb.equal(root.get("approver"), loggedInUser));
            }

            if (filter.getRequestId() != null && !filter.getRequestId().isEmpty()) {
                log.debug("Applying requestId filter | requestId={}", filter.getRequestId());
                predicates.add(cb.equal(root.get("requestId"), filter.getRequestId()));
            }

            if (filter.getApprover() != null && !filter.getApprover().isEmpty()) {
                log.debug("Applying approver filter | approverLike={}", filter.getApprover());
                predicates.add(cb.like(
                        cb.lower(root.get("approver")),
                        "%" + filter.getApprover().toLowerCase() + "%"
                ));
            }


            if (filter.getApprovalStatus() != null && !filter.getApprovalStatus().isEmpty()) {
                log.debug("Applying approvalStatus filter | status={}", filter.getApprovalStatus());
                predicates.add(cb.equal(
                        root.get("approvalStatus"),
                        filter.getApprovalStatus()
                ));
            }

            if (filter.getWorkItemType() != null && !filter.getWorkItemType().isEmpty()) {
                log.debug("Applying workItemType filter | workItemType={}", filter.getWorkItemType());
                predicates.add(cb.equal(
                        root.get("workItemType"),
                        filter.getWorkItemType()
                ));
            }

            if (filter.getWorkItemName() != null && !filter.getWorkItemName().isEmpty()) {
                log.debug("Applying workItemName filter | workItemNameLike={}", filter.getWorkItemName());
                predicates.add(cb.like(
                        cb.lower(root.get("workItemName")),
                        "%" + filter.getWorkItemName().toLowerCase() + "%"
                ));
            }

            if (filter.getFromDate() != null) {
                log.debug("Applying fromDate filter | fromDate={}", filter.getFromDate());
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("approvalRequestDate"),
                        filter.getFromDate()
                ));
            }

            if (filter.getToDate() != null) {
                log.debug("Applying toDate filter | toDate={}", filter.getToDate());
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("approvalRequestDate"),
                        filter.getToDate()
                ));
            }

            log.debug("Total predicates applied | count={}", predicates.size());

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        log.info("END applyFilters | specificationCreated");

        return spec;
    }

}
