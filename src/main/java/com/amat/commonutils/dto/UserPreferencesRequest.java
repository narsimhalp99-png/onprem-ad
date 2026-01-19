package com.amat.commonutils.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPreferencesRequest {

    private String operation;
    private String addFavTiles;
    private String removeFavTiles;
    private String oooApprover;
    private boolean oooEnabled;
    private LocalDateTime oooStartDate;
    private LocalDateTime oooEndDate;

}
