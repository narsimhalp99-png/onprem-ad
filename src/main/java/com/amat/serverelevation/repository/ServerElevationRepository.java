package com.amat.serverelevation.repository;



import com.amat.serverelevation.entity.ServerElevationRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface ServerElevationRepository extends JpaRepository<ServerElevationRequest, Integer> {

    @Modifying
    @Transactional
    @Query("UPDATE ServerElevationRequest e SET e.elevationTime = :time, e.elevationStatus = :status, e.elevationStatusMessage = :msg, e.status = :finalStatus WHERE e.requestId = :requestId")
    void updateOnSuccess(
            @Param("requestId") String requestId,
            @Param("time") LocalDateTime time,
            @Param("status") String status,
            @Param("msg") String msg,
            @Param("finalStatus") String finalStatus);

    @Modifying
    @Transactional
    @Query("UPDATE ServerElevationRequest e SET e.elevationStatus = :elevationStatus, e.elevationStatusMessage = :msg, e.status = :status WHERE e.requestId = :requestId")
    void updateOnFailure(
            @Param("requestId") String requestId,
            @Param("elevationStatus") String elevationStatus,
            @Param("elevationStatusMessage") String elevationStatusMessage,
            @Param("status") String status);

    @Modifying
    @Transactional
    @Query("UPDATE ServerElevationRequest e SET e.status = :status, e.approverId = :approverId WHERE e.requestId = :requestId")
    void updateStatusAndApprover(@Param("requestId") String requestId,
                                 @Param("status") String status,
                                 @Param("approverId") String approverId);
}
