package com.amat.commonutils.service;

import com.amat.accessmanagement.entity.UserEntity;
import com.amat.approvalmanagement.dto.ApprovalWithRequestAndUsersDTO;
import com.amat.commonutils.dto.EmailConfig;
import com.amat.commonutils.entity.SystemConfigurations;
import com.amat.commonutils.repository.SystemConfigurationsRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmailService {

    @Autowired
    JavaMailSender mailSender;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    SystemConfigurationsRepository systemConfigurationsRepository;

    public void sendEmail(String subject, String templateName, ApprovalWithRequestAndUsersDTO variables, UserEntity loggedInUser,UserEntity oldApprover) {

        EmailConfig config = loadEmailConfig();



        if (!isNotificationAllowed(config.getNotificationType())) {
            log.info(
                    "Email notification skipped | NotificationType={}",
                    config.getNotificationType()
            );
            return;
        }

        log.info("START :: Sending email ");
        String to = getToEmail(templateName,variables,config);
        String cc = "";
        String bcc = "";
        if(config.getNotificationType().equalsIgnoreCase("RedirectToEmail")) {
            subject = "[Original Recipient: To: "+ to + ", Cc: " + variables.getRequestorDetails().getEmail() +"]"+ " " +  subject;
        }else{
            cc = variables.getRequestorDetails().getEmail();
            bcc = config.getBccEmail();
        }

        String clickHere = getClickHere(templateName,config, String.valueOf(variables.getApprovalId()));
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            Context context = new Context();

            context.setVariable("content",variables);
            context.setVariable("subject", subject);
            context.setVariable("clickHere", clickHere);
            context.setVariable("oldApprover", oldApprover);

            String htmlContent = templateEngine.process(templateName, context);
            helper.setFrom(config.getFromEmail());
            helper.setTo(to);
            helper.setCc(cc);
            helper.setBcc(bcc);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("SUCCESS :: Email sent | to={}", to);
        } catch (Exception ex) {
            log.error("FAILED :: Email send failed | to={}", to, ex);
            throw new RuntimeException("Failed to send email", ex);
        }
    }

    private static String getToEmail(String templateName, ApprovalWithRequestAndUsersDTO variables, EmailConfig config) {
        String to = "";

        if (config.getNotificationType().equalsIgnoreCase("RedirectToEmail")) {
            to = config.getRedirectionEmail();
        }else if(config.getNotificationType().equalsIgnoreCase("RedirectToUser") && templateName.equalsIgnoreCase("Server-Elevation-ApprovedEmail") || templateName.equalsIgnoreCase("Server-Elevation-ApprovalRequestEmail") || templateName.equalsIgnoreCase("ApprovalReassignedEmail")){
            to = variables.getApproverDetails().getEmail();
        }else{
            to = variables.getRequestorDetails().getEmail();
        }
        return to;
    }

    private boolean isNotificationAllowed(String notificationType) {
        return "RedirectToEmail".equalsIgnoreCase(notificationType)
                || "RedirectToUser".equalsIgnoreCase(notificationType);
    }


    private String getClickHere(String templateName, EmailConfig config, String approvalId) {

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

}
