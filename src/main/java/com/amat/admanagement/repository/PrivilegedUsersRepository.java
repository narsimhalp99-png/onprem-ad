package com.amat.admanagement.repository;


import com.amat.admanagement.model.PrivilegedUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PrivilegedUsersRepository extends JpaRepository<PrivilegedUsers, Long> {
    Optional<PrivilegedUsers> findByUsername(String PrivilegedUsersname);
}