package com.amat.commonutils.dto;


import com.amat.approvalmanagement.dto.ApprovalWithRequestAndUsersDTO;
import io.micrometer.common.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Locale;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequest {

    private String templateName;

    @Nullable
    private ApprovalWithRequestAndUsersDTO variables;

    private String subject;

    private String from;

    private String[] to;

    @Nullable
    private String[] cc;

    @Nullable
    private String[] bcc;

    @Nullable
    private Locale locale;

    @Nullable
    private List<Attachment> attachments;

    @Nullable
    private List<Attachment> inlineResources;

    private String clickHere;

}


