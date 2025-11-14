package com.ldap.userenrollment.dto;

import lombok.Data;

@Data
public class AssignRoleRequest {

    private String roleId;
    private Boolean isRoleActive = true;

}
