package com.amat.accessmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

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

    @JsonIgnore
    @Transient
    private Map<String, Object> ldapData;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String adminAccountDN;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String regularAccountDN;



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
