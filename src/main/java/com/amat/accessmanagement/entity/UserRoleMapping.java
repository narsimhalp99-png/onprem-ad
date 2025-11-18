package com.amat.accessmanagement.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;


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
    @JsonIgnore
    private Long id;

    // FK column mapped as read-only mirror field
    @JsonIgnore
    @Column(name = "employee_id", insertable = false, updatable = false)
    private Long employeeId;

    @Column(name = "AssignedRoleId", nullable = false)
    private String assignedRoleId;

    @JsonIgnore
    @Column(name = "assignedRoleStatus", nullable = false)
    private Boolean assignedRoleStatus;


    // THIS SIDE controls the FK (NO insertable/updatable flags)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference
    private UserEntity user;

}
