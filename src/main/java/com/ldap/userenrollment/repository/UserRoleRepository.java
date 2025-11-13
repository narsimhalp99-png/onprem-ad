package com.ldap.userenrollment.repository;

import com.ldap.userenrollment.entity.UserRoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRoleRepository extends JpaRepository<UserRoleMapping, Long> {
}
