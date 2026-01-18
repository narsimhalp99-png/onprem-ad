package com.amat.commonutil.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class UserPreferencesResponse {
    private String employeeId;
    private List<String> favTiles;
    private boolean oooEnabled;
    private LocalDateTime oooStartDate;
    private LocalDateTime oooEndDate;
    private String oooApprover;
    private String message;
}

