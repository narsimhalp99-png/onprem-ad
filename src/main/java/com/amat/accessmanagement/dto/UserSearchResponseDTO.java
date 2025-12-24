package com.amat.accessmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSearchResponseDTO {
    private String employeeId;
    private String displayName;
    private String email;
}