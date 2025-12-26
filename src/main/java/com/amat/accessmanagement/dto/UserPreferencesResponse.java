package com.amat.accessmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserPreferencesResponse {
    private String employeeId;
    private List<String> favTiles;
}

