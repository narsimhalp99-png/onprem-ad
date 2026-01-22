package com.amat.commonutils.dto;

import lombok.Data;

@Data
public class SystemConfigUpdateRequest {

    private String configType;
    private String configName;
    private String newConfigValue;
}
