package com.amat.commonutil.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailConfig {

    private String fromEmail;
    private String notificationType;
    private String redirectionEmail;
    private String appBaseURL;
    private String bccEmail;
}
