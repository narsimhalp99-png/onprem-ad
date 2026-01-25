package com.amat.commonutils.repository;

import com.amat.commonutils.entity.SystemConfigurations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigurationsRepository extends JpaRepository<SystemConfigurations, Long> {


    List<SystemConfigurations> findAll();

    List<SystemConfigurations> findByConfigType(String configType);

    Optional<SystemConfigurations> findByConfigTypeAndConfigName(
            String configType,
            String configName
    );

}
