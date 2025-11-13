package com.ldap.userenrollment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "Users")
public class UserEntity {

    @Id
    private Long employeeId;

    private String firstName;
    private String lastName;
    private String displayName;
    private String organization;
    private String subOrganization;
    private String title;
    private Long managerEmpId;
    private Boolean isActive;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
