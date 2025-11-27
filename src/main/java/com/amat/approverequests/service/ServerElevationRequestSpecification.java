package com.amat.approverequests.service;

import com.amat.approverequests.dto.getApprovalsDTO;
import com.amat.serverelevation.entity.ServerElevationRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class ServerElevationRequestSpecification {

    public static Specification<ServerElevationRequest> applyFilters(getApprovalsDTO approvalsDTO, String requestedBy, boolean isSelf) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (isSelf) {
                predicates.add(cb.equal(root.get("requestedBy"), requestedBy));
            }

            if (approvalsDTO.getFilter().getServerName() != null) {
                predicates.add(cb.like(cb.lower(root.get("serverName")),
                        "%" + approvalsDTO.getFilter().getServerName().toLowerCase() + "%"));
            }

            if (approvalsDTO.getFilter().getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), approvalsDTO.getFilter().getStatus()));
            }

            if (approvalsDTO.getFilter().getElevationStatus() != null) {
                predicates.add(cb.equal(root.get("elevationStatus"), approvalsDTO.getFilter().getElevationStatus()));
            }

//            if (approvalsDTO.getFilter().getDeElevationStatus() != null) {
//                predicates.add(cb.equal(root.get("deElevationStatus"), approvalsDTO.getFilter().getDeElevationStatus()));
//            }

            if (approvalsDTO.getFilter().getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestDate"), approvalsDTO.getFilter().getFromDate()));
            }

            if (approvalsDTO.getFilter().getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestDate"), approvalsDTO.getFilter().getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

