package com.amat.serverelevation.entity;

import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.Data;
import org.hibernate.annotations.GenerationTime;

import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "server_elevation_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerElevationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false,length = 100)
    private String requestId;

    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private String serverName;

    @Column(nullable = false)
    private Integer durationInHours;

    @Column(columnDefinition = "nvarchar(max)")
    private String requestorComment;

    @Generated(GenerationTime.INSERT)
    @Column(
            name = "requestDate",
            nullable = false,
            updatable = false,
            insertable = false,
            columnDefinition = "DATETIME DEFAULT GETDATE()"
    )
    private LocalDateTime requestDate;

    private LocalDateTime elevationTime;

    private String elevationStatus;

    private LocalDateTime deElevationTime;

    private String deElevationStatus;

    @Column(columnDefinition = "nvarchar(max)")
    private String elevationStatusMessage;

    @Column(columnDefinition = "nvarchar(max)")
    private String deElevationStatusMessage;

    @Column(nullable = false)
    private String status;

    private String approverId;



}
