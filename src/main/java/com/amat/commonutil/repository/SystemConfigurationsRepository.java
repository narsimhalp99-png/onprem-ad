package com.amat.commonutil.repository;

import com.amat.commonutil.entity.SystemConfigurations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigurationsRepository extends JpaRepository<SystemConfigurations, String> {


    List<SystemConfigurations> findByConfigType(String configType);

    Optional<SystemConfigurations> findByConfigTypeAndConfigName(
            String configType,
            String configName
    );

}
