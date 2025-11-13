package com.ldap.userenrollment.dto;

import lombok.Data;

@Data
public class AssignRoleRequest {

    private Long roleId;
    private String roleName;
    private Boolean isRoleActive = true;

}
