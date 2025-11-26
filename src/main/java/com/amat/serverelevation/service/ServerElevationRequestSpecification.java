package com.amat.serverelevation.service;

import com.amat.serverelevation.DTO.ServerElevationRequestFilterDTO;
import com.amat.serverelevation.entity.ServerElevationRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class ServerElevationRequestSpecification {

    public static Specification<ServerElevationRequest> applyFilters(ServerElevationRequestFilterDTO filter, String requestedBy, boolean isSelf) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (isSelf) {
                predicates.add(cb.equal(root.get("requestedBy"), requestedBy));
            }

            if (filter.getServerName() != null) {
                predicates.add(cb.like(cb.lower(root.get("serverName")),
                        "%" + filter.getServerName().toLowerCase() + "%"));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getElevationStatus() != null) {
                predicates.add(cb.equal(root.get("elevationStatus"), filter.getElevationStatus()));
            }

            if (filter.getDeElevationStatus() != null) {
                predicates.add(cb.equal(root.get("deElevationStatus"), filter.getDeElevationStatus()));
            }

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestDate"), filter.getFromDate()));
            }

            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestDate"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

