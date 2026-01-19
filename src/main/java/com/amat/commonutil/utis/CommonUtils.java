package com.amat.commonutil.utis;


import com.amat.approvalmanagement.dto.ApprovalWithRequestAndUsersDTO;
import com.amat.commonutil.dto.EmailConfig;
import com.amat.commonutil.dto.EmailRequest;
import com.amat.commonutil.entity.SystemConfigurations;
import com.amat.commonutil.entity.UserPreferences;
import com.amat.commonutil.repository.SystemConfigurationsRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommonUtils {


    @Autowired
    SystemConfigurationsRepository systemConfigurationsRepository;

    public boolean isUserOutOfOffice(UserPreferences prefs) {
        if (prefs == null) return false;

        if (!Boolean.TRUE.equals(prefs.isOooEnabled())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        return prefs.getOooStartDate() != null
                && prefs.getOooEndDate() != null
                && !now.isBefore(prefs.getOooStartDate())
                && !now.isAfter(prefs.getOooEndDate());
    }

    public static String[] getToEmail(
            String templateName,
            ApprovalWithRequestAndUsersDTO variables,
            EmailConfig config
    ) {
        if ("RedirectToEmail".equalsIgnoreCase(config.getNotificationType())) {
            return new String[]{ config.getRedirectionEmail()}; // already String[]
        }

        if (
                "RedirectToUser".equalsIgnoreCase(config.getNotificationType()) &&
                        (
                                "Server-Elevation-ApprovedEmail".equalsIgnoreCase(templateName) ||
                                        "Server-Elevation-ApprovalRequestEmail".equalsIgnoreCase(templateName) ||
                                        "ApprovalReassignedEmail".equalsIgnoreCase(templateName)
                        )
        ) {
            return new String[]{ variables.getApproverDetails().getEmail() };
        }

        return new String[]{ variables.getRequestorDetails().getEmail() };
    }


    public boolean isNotificationAllowed(String notificationType) {
        return "RedirectToEmail".equalsIgnoreCase(notificationType)
                || "RedirectToUser".equalsIgnoreCase(notificationType);
    }


    public String getClickHere(String templateName, EmailConfig config, String approvalId) {

        String baseUrl = config.getAppBaseURL();

        if (templateName == null || baseUrl == null) {
            return "";
        }

        // Approval / Reassign emails
        if (templateName.equalsIgnoreCase("Server-Elevation-ApprovalRequestEmail")
                || templateName.equalsIgnoreCase("ApprovalReassignedEmail")) {

            return baseUrl + "approvals?" + approvalId;
        }

        // Approved / Rejected emails to requestor
        if (templateName.equalsIgnoreCase("Server-Elevation-ApprovedEmail")
                || templateName.equalsIgnoreCase("RejectedEmail")) {

            return baseUrl + "server-elevation#myview";
        }

        return "";
    }



    public EmailConfig loadEmailConfig() {

        List<SystemConfigurations> configs =
                systemConfigurationsRepository.findByConfigType("EmailConfiguration");

        Optional<SystemConfigurations> appBaseUrlConfig =
                systemConfigurationsRepository.findByConfigTypeAndConfigName(
                        "GeneralConfiguration",
                        "appBaseURL"
                );

        String appBaseURL = appBaseUrlConfig
                .map(SystemConfigurations::getConfigValue)
                .orElse(null);

        log.info("appBaseURL from db:::{}", appBaseURL);

        Map<String, String> map = configs.stream()
                .collect(Collectors.toMap(
                        SystemConfigurations::getConfigName,
                        SystemConfigurations::getConfigValue
                ));

        return new EmailConfig(
                map.get("FromEmail"),
                map.get("NotificationType"),
                map.get("RedirectToEmail"), // Redirection email
                appBaseURL,
                map.get("BccEmail")         // optional
        );
    }

    public EmailRequest prepareEmailRequest(EmailRequest emailRequest) {

        EmailConfig config = loadEmailConfig();

        String[] to = getToEmail(emailRequest.getTemplateName(),emailRequest.getVariables(),config);
        String[] cc = {};
        String[] bcc = {};
        String subject = "";

        assert emailRequest.getVariables() != null;
        if ("RedirectToEmail".equalsIgnoreCase(config.getNotificationType())) {

            subject = "[Original Recipient: To: "
                    + Arrays.toString(to)
                    + ", Cc: "
                    + emailRequest.getVariables().getRequestorDetails().getEmail()
                    + "] "
                    + emailRequest.getSubject();

        } else {

            cc = new String[]{
                    emailRequest.getVariables().getRequestorDetails().getEmail()
            };

            bcc = new String[]{config.getBccEmail()};
        }

        emailRequest.setSubject(subject);
        emailRequest.setCc(cc);
        emailRequest.setBcc(bcc);

        String clickHere = getClickHere(emailRequest.getTemplateName(), config, String.valueOf(emailRequest.getVariables().getApprovalId()));
        emailRequest.setClickHere(clickHere);

        return emailRequest;

    }



}
