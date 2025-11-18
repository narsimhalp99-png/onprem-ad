package com.amat.accessmanagement.repository;

import com.amat.accessmanagement.entity.UserRoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleMappingRepository extends JpaRepository<UserRoleMapping, String> {


    UserRoleMapping findByEmployeeIdAndAssignedRoleId(Long employeeId, String assignedRoleId);

    List<UserRoleMapping> findByEmployeeId(Long employeeId);

    void deleteByEmployeeIdAndAssignedRoleId(Long employeeId, String assignedRoleId);
}
