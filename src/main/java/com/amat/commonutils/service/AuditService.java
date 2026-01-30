package com.amat.commonutils.service;


import com.amat.commonutils.entity.Audit;
import com.amat.commonutils.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditAdOperation(
            String source,
            String actor,
            String target,
            String operation,
            String objectName,
            String oldValue,
            String newValue,
            String comment
    ) {
        try {
            Audit audit = Audit.builder()
                    .source(source)
                    .actor(actor)
                    .target(target)
                    .operation(operation)
                    .objectName(objectName)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .comment(comment)
                    .build();

            auditRepository.save(audit);

            log.info(
                    "AUDIT SUCCESS | source={} | operation={} | target={}",
                    source, operation, target
            );

        } catch (Exception ex) {
            log.error(
                    "AUDIT FAILURE | source={} | operation={} | target={} | error={}",
                    source, operation, target, ex.getMessage(), ex
            );
        }
    }
}

