package com.amat.accessmanagement.repository;

import com.amat.accessmanagement.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserEnrollmentRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = "roles")
    Page<UserEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByEmployeeId(Long id);

}
