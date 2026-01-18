package com.amat.commonutil.repository;



import com.amat.accessmanagement.entity.PrivilegedUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PrivilegedUsersRepository extends JpaRepository<PrivilegedUsers, Long> {
    Optional<PrivilegedUsers> findByUsername(String PrivilegedUsersname);
}