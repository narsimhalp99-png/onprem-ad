package com.amat.serverelevation.service;

import com.amat.serverelevation.DTO.getServerElevationRequests;
import com.amat.serverelevation.entity.ServerElevationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

@Slf4j
public class ServerElevationRequestSpecification {

    public static Specification<ServerElevationRequest> applyFilters(
            getServerElevationRequests approvalsDTO,
            String requestedBy,
            boolean isSelf) {

        log.info(
                "START applyFilters (ServerElevationRequest) | requestedBy={} | isSelf={} | filter={}",
                requestedBy,
                isSelf,
                approvalsDTO.getFilter()
        );

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (isSelf) {
                log.debug(
                        "Applying self filter | requestedBy={}",
                        requestedBy
                );
                predicates.add(cb.equal(root.get("requestedBy"), requestedBy));
            }

            if (approvalsDTO.getFilter().getServerName() != null
                    && !approvalsDTO.getFilter().getServerName().isEmpty()) {

                log.debug(
                        "Applying serverName filter (1) | serverNameLike={}",
                        approvalsDTO.getFilter().getServerName()
                );

                predicates.add(cb.like(
                        cb.lower(root.get("serverName")),
                        "%" + approvalsDTO.getFilter().getServerName().toLowerCase() + "%"
                ));
            }

            if (approvalsDTO.getFilter().getServerName() != null
                    && !approvalsDTO.getFilter().getServerName().isEmpty()) {

                log.debug(
                        "Applying serverName filter (2) | serverNameLike={}",
                        approvalsDTO.getFilter().getServerName()
                );

                predicates.add(cb.like(
                        cb.lower(root.get("serverName")),
                        "%" + approvalsDTO.getFilter().getServerName().toLowerCase() + "%"
                ));
            }

            if (approvalsDTO.getFilter().getStatus() != null
                    && !approvalsDTO.getFilter().getStatus().isEmpty()) {

                log.debug(
                        "Applying status filter | status={}",
                        approvalsDTO.getFilter().getStatus()
                );

                predicates.add(cb.equal(
                        root.get("status"),
                        approvalsDTO.getFilter().getStatus()
                ));
            }

            if (approvalsDTO.getFilter().getRequestId() != null
                    && !approvalsDTO.getFilter().getRequestId().isEmpty()) {

                log.debug(
                        "Applying requestId filter | requestId={}",
                        approvalsDTO.getFilter().getRequestId()
                );

                predicates.add(cb.equal(
                        root.get("requestId"),
                        approvalsDTO.getFilter().getRequestId()
                ));
            }

            if (approvalsDTO.getFilter().getElevationStatus() != null
                    && !approvalsDTO.getFilter().getElevationStatus().isEmpty()) {

                log.debug(
                        "Applying elevationStatus filter | elevationStatus={}",
                        approvalsDTO.getFilter().getElevationStatus()
                );

                predicates.add(cb.equal(
                        root.get("elevationStatus"),
                        approvalsDTO.getFilter().getElevationStatus()
                ));
            }

            if (approvalsDTO.getFilter().getDeElevationStatus() != null
                    && !approvalsDTO.getFilter().getDeElevationStatus().isEmpty()) {

                log.debug(
                        "Applying deElevationStatus filter | deElevationStatus={}",
                        approvalsDTO.getFilter().getDeElevationStatus()
                );

                predicates.add(cb.equal(
                        root.get("deElevationStatus"),
                        approvalsDTO.getFilter().getDeElevationStatus()
                ));
            }

            if (approvalsDTO.getFilter().getFromDate() != null) {

                log.debug(
                        "Applying fromDate filter | fromDate={}",
                        approvalsDTO.getFilter().getFromDate()
                );

                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("requestDate"),
                        approvalsDTO.getFilter().getFromDate()
                ));
            }

            if (approvalsDTO.getFilter().getToDate() != null) {

                log.debug(
                        "Applying toDate filter | toDate={}",
                        approvalsDTO.getFilter().getToDate()
                );

                predicates.add(cb.lessThanOrEqualTo(
                        root.get("requestDate"),
                        approvalsDTO.getFilter().getToDate()
                ));
            }

            log.debug(
                    "Total predicates applied | count={}",
                    predicates.size()
            );

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
