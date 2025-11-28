package com.amat.serverelevation.repository;

import com.amat.serverelevation.entity.ServerElevationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerElevationRequestRepository extends
        JpaRepository<ServerElevationRequest, Integer>,
        JpaSpecificationExecutor<ServerElevationRequest> {
}