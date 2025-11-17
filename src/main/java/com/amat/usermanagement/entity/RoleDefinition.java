package com.amat.usermanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "roles")
public class RoleDefinition {

    @Id
    @Column(unique = true, nullable = false)
    private String roleId;

    @Column(unique = true, nullable = false)
    private String roleName;

    private String roleDescription;

}
