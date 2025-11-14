package com.ldap.userenrollment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
@Data
@Entity
@Table(name = "UserRoleMapping",
        uniqueConstraints = @UniqueConstraint(name = "UQ_UserRole_Employee_Role",
                columnNames = {"EmployeeId", "AssignedRoleId"}))
public class UserRoleMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "EmployeeId", nullable = false)
    private Long employeeId;

    @Column(name = "AssignedRoleId", nullable = false)
    private String assignedRoleId;

    @Column(name = "assignedRoleStatus", nullable = false)
    private Boolean assignedRoleStatus;

    @Column(name = "AssignedAt", updatable = false)
    private OffsetDateTime assignedAt;

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = OffsetDateTime.now();
        }
    }
}
