package com.amat.accessmanagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPreferencesRequest {
    private String addFavTiles;
    private String removeFavTiles;
}
