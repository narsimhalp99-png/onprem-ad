package com.amat.commonutils.dto;


import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.InputStreamSource;

@Data
@Builder
public class Attachment {

    private String filename;
    private InputStreamSource source;
    private String contentType;
}