package com.ldap.myidcustomerservice.dto;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ModifyGroupResponse {
    String message;
    String statusCode;
    List<String> userNotExist;
    List<String> errors= new ArrayList<>();
}
