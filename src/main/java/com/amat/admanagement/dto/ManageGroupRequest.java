package com.amat.admanagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class ManageGroupRequest {
    private String groupDn;
    private List<String> userDns;
    private String operation;
}
