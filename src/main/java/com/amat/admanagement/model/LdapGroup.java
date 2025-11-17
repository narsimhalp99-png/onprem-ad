package com.amat.admanagement.model;

import lombok.Data;

@Data
public class LdapGroup {
    private String dn;
    private String cn;
    private String description;
}
