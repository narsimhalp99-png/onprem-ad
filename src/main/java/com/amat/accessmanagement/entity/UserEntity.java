package com.amat.accessmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "Users")
public class UserEntity {

    @Id
    private String employeeId;

    private String firstName;
    private String lastName;
    private String email;
    private String displayName;
    private String organization;
    private String subOrganization;
    private String title;
    private String managerEmpId;
    private Boolean isActive;

    @Generated(GenerationTime.INSERT)
    @Column(
            name = "createdAt",
            updatable = false,
            insertable = false
    )
    private LocalDateTime createdAt;


    private OffsetDateTime updatedAt;


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    @Where(clause = "assigned_role_status = true")
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
    @Transient
    @JsonProperty("roles")
    public List<String> getRoleNames() {
        if (roles == null) return List.of();
        return roles.stream()
                .map(UserRoleMapping::getAssignedRoleId)
                .toList();
    }
}
