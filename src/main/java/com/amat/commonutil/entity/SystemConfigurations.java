package com.amat.commonutil.entity;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "system_configurations")
public class SystemConfigurations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String configType;

    private String configName;

    private String configValue;


}


