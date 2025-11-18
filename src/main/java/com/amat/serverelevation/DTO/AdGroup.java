package com.amat.serverelevation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdGroup {
    private String name;
    private String managedBy; // Distinguished Name (DN)
}
