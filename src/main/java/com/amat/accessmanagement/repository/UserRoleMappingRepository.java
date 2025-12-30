package com.amat.accessmanagement.repository;

import com.amat.accessmanagement.entity.UserRoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRoleMappingRepository extends JpaRepository<UserRoleMapping, String> {


    UserRoleMapping findByEmployeeIdAndAssignedRoleId(String employeeId, String assignedRoleId);


    boolean existsByUserEmployeeIdAndAssignedRoleId(
            String employeeId,
            String assignedRoleId
    );

    void deleteByUserEmployeeIdAndAssignedRoleId(
            String employeeId,
            String assignedRoleId
    );

}
