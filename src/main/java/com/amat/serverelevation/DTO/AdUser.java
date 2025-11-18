package com.amat.serverelevation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdUser {
    private String dn;
    private String employeeId;
    private String displayName;
}
