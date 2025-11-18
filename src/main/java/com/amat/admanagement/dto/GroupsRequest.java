package com.amat.admanagement.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupsRequest {

    String filter;
    int pageNumber;
    int pageSize;
    String searchBaseOU;
    List<String> addtnlAttributes;
    boolean fetchRecursiveMembers;

}
