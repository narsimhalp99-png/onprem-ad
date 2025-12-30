package com.amat.accessmanagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateRolesRequest {


    private String operation;

    private List<String> roles;


}
