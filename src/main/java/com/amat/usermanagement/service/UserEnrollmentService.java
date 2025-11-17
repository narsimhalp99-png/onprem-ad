package com.amat.usermanagement.service;

import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.UserService;
import com.amat.usermanagement.entity.UserEntity;
import com.amat.usermanagement.exception.NotFoundException;
import com.amat.usermanagement.repository.RoleRepository;
import com.amat.usermanagement.repository.UserEnrollmentRepository;
import com.amat.usermanagement.repository.UserRoleMappingRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Data
@Service
@Slf4j
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

    public UserEntity getUser(Long employeeId, boolean additionalDetails) {

        // Fetch user from DB
        UserEntity user = userRepo.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("User not found: " + employeeId));

        if (additionalDetails) {

            try {
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
            }catch(Exception e){
                log.info("Exception details are::{}", e);
            }
        }

        // If additionalDetails == false, don't attach LDAP data at all
        return user;
    }


    public UserEntity updateUser(Long employeeId, UserEntity update) {
        UserEntity existing = getUser(employeeId,false);
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
        UserEntity existing = getUser(employeeId,false);
        userRepo.delete(existing);
    }



}
