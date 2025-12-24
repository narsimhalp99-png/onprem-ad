package com.amat.accessmanagement.repository;

import com.amat.accessmanagement.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserEnrollmentRepository extends JpaRepository<UserEntity, String> {

    @EntityGraph(attributePaths = "roles")
    Page<UserEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByEmployeeId(String id);


    @Query("""
        SELECT u FROM UserEntity u
        WHERE (:search IS NULL OR
               LOWER(u.displayName) LIKE LOWER(CONCAT(:search, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT(:search, '%')) OR
               LOWER(u.employeeId) LIKE LOWER(CONCAT(:search, '%')))
        ORDER BY u.displayName
    """)
    Page<UserEntity> searchUsers(String search, Pageable pageable);

}
