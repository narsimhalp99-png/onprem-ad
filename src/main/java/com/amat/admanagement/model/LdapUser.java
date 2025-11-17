package com.amat.admanagement.model;

import lombok.Data;

@Data
public class LdapUser {
    private String dn;
    private String cn;
    private String sAMAccountName;
    private String mail;
}
