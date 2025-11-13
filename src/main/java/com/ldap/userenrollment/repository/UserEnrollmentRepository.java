package com.ldap.userenrollment.repository;

import com.ldap.userenrollment.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEnrollmentRepository extends JpaRepository<UserEntity, Long> {
}
