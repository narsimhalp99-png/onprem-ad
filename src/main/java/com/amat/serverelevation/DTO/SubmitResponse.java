package com.amat.serverelevation.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitResponse {

    private String serverName;
    private String status;
    private String requestId;
    private String approvalId;
    private String message;
}
