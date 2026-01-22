package com.amat.commonutils.service;


import com.amat.commonutils.dto.SystemConfigUpdateRequest;
import com.amat.commonutils.entity.SystemConfigurations;
import com.amat.commonutils.repository.SystemConfigurationsRepository;
import com.amat.commonutils.service.SystemConfigurationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigurationsService {

    private final SystemConfigurationsRepository repository;

    public List<SystemConfigurations> getAllConfigs() {
        log.info("Fetching all system configurations");
        return repository.findAll();
    }


    public List<SystemConfigurations> getConfigsByType(String configType) {
        log.info("Fetching system configurations for type={}", configType);
        return repository.findByConfigType(configType);
    }

    public SystemConfigurations updateConfigValue(
            SystemConfigUpdateRequest request) {

        log.info(
                "Updating config | type={} | name={}",
                request.getConfigType(),
                request.getConfigName()
        );

        SystemConfigurations config = repository
                .findByConfigTypeAndConfigName(
                        request.getConfigType(),
                        request.getConfigName()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Configuration not found for given type and name"
                        )
                );

        config.setConfigValue(request.getNewConfigValue());

        return repository.save(config);
    }
}
