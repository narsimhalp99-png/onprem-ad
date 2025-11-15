package com.ldap.userenrollment.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<UserRoleMapping> roles;

    @Transient
    private Map<String, Object> ldapData;


    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public List<UserRoleMapping> getRoles() {
        if (roles == null) return null;
        return roles.stream()
                .filter(UserRoleMapping::getAssignedRoleStatus)
                .toList();
    }
}
