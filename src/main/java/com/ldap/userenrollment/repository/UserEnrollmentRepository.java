package com.ldap.userenrollment.repository;

import com.ldap.userenrollment.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserEnrollmentRepository extends JpaRepository<UserEntity, Long> {

}
