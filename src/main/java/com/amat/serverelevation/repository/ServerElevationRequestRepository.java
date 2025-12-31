package com.amat.serverelevation.repository;

import com.amat.serverelevation.entity.ServerElevationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerElevationRequestRepository extends
        JpaRepository<ServerElevationRequest, Integer>,
        JpaSpecificationExecutor<ServerElevationRequest> {


    Optional<ServerElevationRequest> findByRequestId(String requestId);


    List<ServerElevationRequest> findByRequestIdIn(List<String> requestIds);

}