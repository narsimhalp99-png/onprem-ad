package com.amat.usermanagement.dto;

import lombok.Data;

@Data
public class User {
    private Long employeeId;
    private String displayName;
    private String firstName;
    private String lastName;
    private String org;
    private String subOrg;
    private String title;
    private Long managerId;
    private Boolean isActive;

}
