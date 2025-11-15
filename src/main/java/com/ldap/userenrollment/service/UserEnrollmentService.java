package com.ldap.userenrollment.service;

import com.ldap.myidcustomerservice.dto.UsersRequest;
import com.ldap.myidcustomerservice.service.UserService;
import com.ldap.userenrollment.dto.AssignRoleRequest;
import com.ldap.userenrollment.entity.UserEntity;
import com.ldap.userenrollment.entity.UserRoleMapping;
import com.ldap.userenrollment.exception.NotFoundException;
import com.ldap.userenrollment.repository.RoleRepository;
import com.ldap.userenrollment.repository.UserEnrollmentRepository;
import com.ldap.userenrollment.repository.UserRoleMappingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@Service
public class UserEnrollmentService {

    @Autowired
    UserEnrollmentRepository userRepo;

    @Autowired
    RoleRepository roleRepo;

    @Autowired
    UserRoleMappingRepository userRoleRepo;

    @Autowired
    UserService ldapUserService;

    @Value("${spring.ldap.base:''}")
    String defaultBase;



    public UserEntity createUser(UserEntity user) {
        return userRepo.save(user);
    }

    public Page<UserEntity> getUsers(Pageable pageable) {
        return userRepo.findAll(pageable);
    }

    public UserEntity getUser(Long employeeId) {


        UserEntity user = userRepo.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("User not found: " + employeeId));

        UsersRequest firstReq = new UsersRequest();
        firstReq.setSearchBaseOU(defaultBase);
        firstReq.setFilter("employeeId=" + employeeId);
        firstReq.setPageNumber(0);
        firstReq.setPageSize(5);
        firstReq.setAddtnlAttributes(
                List.of("manager", "employeeType", "userAccountControl")
        );

        Map<String, Object> firstResp = ldapUserService.fetchAllObjects(firstReq);

        // Extract "distinguishedName" from first response
        List<Map<String, Object>> dataList =
                (List<Map<String, Object>>) firstResp.get("data");

        if (dataList == null || dataList.isEmpty()) {
            throw new NotFoundException("LDAP user not found for employeeId: " + employeeId);
        }

        String distinguishedName = (String) dataList.get(0).get("distinguishedName");


        UsersRequest secondReq = new UsersRequest();
        secondReq.setSearchBaseOU(distinguishedName);
        secondReq.setFilter("(&(manager=" + distinguishedName + ")(employeeType=SA)(!(userAccountControl=514)))");
        secondReq.setPageNumber(0);
        secondReq.setPageSize(5);
        secondReq.setAddtnlAttributes(
                List.of("manager", "employeeType", "userAccountControl")
        );

        Map<String, Object> finalLdapResp = ldapUserService.fetchAllObjects(secondReq);


        user.setLdapData(finalLdapResp);

        return user;
    }


    public UserEntity updateUser(Long employeeId, UserEntity update) {
        UserEntity existing = getUser(employeeId);
        if (update.getDisplayName() != null) existing.setDisplayName(update.getDisplayName());
        existing.setFirstName(update.getFirstName());
        existing.setLastName(update.getLastName());
        existing.setOrganization(update.getOrganization());
        existing.setSubOrganization(update.getSubOrganization());
        existing.setTitle(update.getTitle());
        existing.setManagerEmpId(update.getManagerEmpId());
        existing.setIsActive(update.getIsActive());
        return userRepo.save(existing);
    }

    public void deleteUser(Long employeeId) {
        UserEntity existing = getUser(employeeId);
        userRepo.delete(existing);
    }



}
