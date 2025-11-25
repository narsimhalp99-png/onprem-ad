package com.amat.serverelevation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Data;

import java.time.LocalDateTime;


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

    // DB generates the RequestID via DEFAULT NEWID(); JPA should not try to insert value
    @Column(insertable = false, updatable = false)
    private String requestId;

    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private String serverName;

    @Column(nullable = false)
    private Integer durationInHours;

    @Column(columnDefinition = "nvarchar(max)")
    private String requestorComment;

    @Column(insertable = false, updatable = false)
    private LocalDateTime requestDate;

    private LocalDateTime elevationTime;

    private String elevationStatus;

    @Column(columnDefinition = "nvarchar(max)")
    private String elevationStatusMessage;

    @Column(nullable = false)
    private String status;

    private String approverId;


}
