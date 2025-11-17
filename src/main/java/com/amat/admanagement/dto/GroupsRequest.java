package com.amat.admanagement.dto;


import lombok.Data;

import java.util.List;

@Data
public class GroupsRequest {

    String filter;
    int pageNumber;
    int pageSize;
    String searchBaseOU;
    List<String> addtnlAttributes;
    boolean fetchRecursiveMembers;

}
