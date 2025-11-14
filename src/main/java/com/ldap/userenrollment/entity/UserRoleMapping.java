package com.ldap.userenrollment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "UserRoleMapping",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_UserRole_Employee_Role",
                columnNames = {"employee_id", "AssignedRoleId"}
        )
)
public class UserRoleMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK column mapped as read-only mirror field
    @Column(name = "employee_id", insertable = false, updatable = false)
    private Long employeeId;

    @Column(name = "AssignedRoleId", nullable = false)
    private String assignedRoleId;

    @Column(name = "assignedRoleStatus", nullable = false)
    private Boolean assignedRoleStatus;

    @Column(name = "AssignedAt", updatable = false)
    private OffsetDateTime assignedAt;

    // THIS SIDE controls the FK (NO insertable/updatable flags)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference
    private UserEntity user;

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = OffsetDateTime.now();
        }
    }
}
