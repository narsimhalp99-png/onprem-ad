package com.amat.commonutils.service;

import com.amat.commonutils.dto.Attachment;
import com.amat.commonutils.dto.EmailConfig;
import com.amat.commonutils.dto.EmailRequest;
import com.amat.commonutils.repository.SystemConfigurationsRepository;
import com.amat.commonutils.util.CommonUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class EmailService {

    @Autowired
    JavaMailSender mailSender;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    SystemConfigurationsRepository systemConfigurationsRepository;

    @Autowired
    CommonUtils commonUtils;

    public void sendEmail(EmailRequest emailRequest) {

        EmailConfig config = commonUtils.loadEmailConfig();


        if (!commonUtils.isNotificationAllowed(config.getNotificationType())) {
            log.info(
                    "Email notification skipped | NotificationType={}",
                    config.getNotificationType()
            );
            return;
        }

        log.info("START :: Sending email ");

        try {
            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            Context context = new Context();

            context.setVariable("content",emailRequest.getVariables());
            context.setVariable("subject", emailRequest.getSubject());
            context.setVariable("clickHere", emailRequest.getClickHere());
//            context.setVariable("oldApprover", oldApprover);

            String htmlContent = templateEngine.process(emailRequest.getTemplateName(), context);
            helper.setFrom(config.getFromEmail());
            helper.setTo(emailRequest.getTo());
            helper.setCc(emailRequest.getCc()!=null?emailRequest.getCc(): new String[0]);
            helper.setBcc(emailRequest.getBcc()!=null?emailRequest.getBcc(): new String[0]);
            helper.setSubject(emailRequest.getSubject());
            helper.setText(htmlContent, true);
            if (emailRequest.getAttachments() != null) {
                for (Attachment a : emailRequest.getAttachments()) {
                    helper.addAttachment(a.getFilename(), a.getSource(), a.getContentType());
                }
            }

            // Optional inline resources (e.g., images referenced by cid:logo)
            if (emailRequest.getInlineResources() != null) {
                for (Attachment a : emailRequest.getInlineResources()) {
                    helper.addInline(a.getFilename(), a.getSource(), a.getContentType());
                }
            }

            mailSender.send(message);
            log.info("SUCCESS :: Email sent | to={}", emailRequest.getTo());
        } catch (Exception ex) {
            log.error("FAILED :: Email send failed | to={}", emailRequest.getTo(), ex);
            throw new RuntimeException("Failed to send email", ex);
        }
    }



}
