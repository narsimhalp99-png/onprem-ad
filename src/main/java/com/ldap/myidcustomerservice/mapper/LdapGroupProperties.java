package com.ldap.myidcustomerservice.mapper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ldap.group")
public class LdapGroupProperties {
    private List<String> defaultAttributes;
}
