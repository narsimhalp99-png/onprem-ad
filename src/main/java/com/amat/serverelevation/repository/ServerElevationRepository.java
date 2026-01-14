package com.amat.serverelevation.repository;



import com.amat.serverelevation.entity.ServerElevationRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerElevationRepository extends JpaRepository<ServerElevationRequest, Integer> {

    @Modifying
    @Transactional
    @Query("UPDATE ServerElevationRequest e SET e.elevationTime = :elevationTime, e.elevationStatus = :elevationStatus, e.elevationStatusMessage = :elevationStatusMessage, e.status = :status WHERE e.requestId = :requestId")
    void updateOnSuccess(
            @Param("requestId") String requestId,
            @Param("elevationTime") LocalDateTime elevationTime,
            @Param("elevationStatus") String elevationStatus,
            @Param("elevationStatusMessage") String elevationStatusMessage,
            @Param("status") String status);

    @Modifying
    @Transactional
    @Query("UPDATE ServerElevationRequest e SET e.elevationStatus = :elevationStatus, e.elevationStatusMessage = :elevationStatusMessage, e.status = :status WHERE e.requestId = :requestId")
    void updateOnFailure(
            @Param("requestId") String requestId,
            @Param("elevationStatus") String elevationStatus,
            @Param("elevationStatusMessage") String elevationStatusMessage,
            @Param("status") String status);

    @Modifying
    @Transactional
    @Query("UPDATE ServerElevationRequest e SET e.status = :status, e.approvalId = :approvalId WHERE e.requestId = :requestId")
    void updateStatusAndApprover(@Param("requestId") String requestId,
                                 @Param("status") String status,
                                 @Param("approvalId") String approvalId);

    @Modifying
    @Transactional
    @Query("""
        update ServerElevationRequest s
           set s.approvalId = :newApprovalId
         where s.requestId = :requestId
    """)
    int updateApprovalId(@Param("requestId") String requestId,
                         @Param("newApprovalId") String newApprovalId);


    Optional<ServerElevationRequest> findByRequestId(String requestId);


    Optional<ServerElevationRequest> findByRequestedByAndServerNameAndStatus(
            String requestedBy,
            String serverName,
            String status
    );

}
