package com.amat.accessmanagement.service;

import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.UserService;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.exception.NotFoundException;
import com.amat.accessmanagement.repository.RoleRepository;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.accessmanagement.repository.UserRoleMappingRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public UserEntity getUser(String employeeId, boolean additionalDetails) {

        UserEntity user = userRepo.findById(employeeId).orElse(null);

        if(user==null){
            return null;
        }

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

                String distinguishedName = (String) dataList.get(0).get("distinguishedName");

                if (!dataList.isEmpty() && distinguishedName!=null){
                    user.setRegularAccountDN(distinguishedName);
                }

                if (dataList == null || dataList.isEmpty()) {
                    throw new NotFoundException("LDAP user not found for employeeId: " + employeeId);
                }

                UsersRequest secondReq = new UsersRequest();
                secondReq.setSearchBaseOU(distinguishedName);
                secondReq.setFilter("(&(manager=" + distinguishedName + ")(employeeType=SA)(!(userAccountControl=514)))");
                secondReq.setPageNumber(0);
                secondReq.setPageSize(5);
                secondReq.setAddtnlAttributes(
                        List.of("manager", "employeeType", "userAccountControl")
                );

                Map<String, Object> finalLdapResp = ldapUserService.fetchAllObjects(secondReq);


                List<Map<String, Object>> newDataList = (List<Map<String, Object>>) finalLdapResp.get("data");

                Map<String, Object> firstEntry = newDataList.get(0);


                String adminDN = (String) firstEntry.get("distinguishedName");

                if(!adminDN.isEmpty() && !adminDN.isBlank())
                user.setAdminAccountDN(adminDN);



//                user.setLdapData(finalLdapResp);
            }catch(Exception e){
                log.info("Exception details are::{}", e);
            }
        }

        // If additionalDetails == false, don't attach LDAP data at all
        return user;
    }


    public UserEntity updateUser(String employeeId, UserEntity update) {
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

    public void deleteUser(String employeeId) {
        UserEntity existing = getUser(employeeId,false);
        userRepo.delete(existing);
    }

    public void updateUserDetails(String employeeId, UserEntity update) {

        UserEntity existing = getUser(employeeId, false);

        if (update.getDisplayName() != null)
            existing.setDisplayName(update.getDisplayName());

        if (update.getFirstName() != null)
            existing.setFirstName(update.getFirstName());

        if (update.getLastName() != null)
            existing.setLastName(update.getLastName());

        if (update.getOrganization() != null)
            existing.setOrganization(update.getOrganization());

        if (update.getSubOrganization() != null)
            existing.setSubOrganization(update.getSubOrganization());

        if (update.getTitle() != null)
            existing.setTitle(update.getTitle());

        if (update.getManagerEmpId() != null)
            existing.setManagerEmpId(update.getManagerEmpId());

        if (update.getIsActive() != null)
            existing.setIsActive(update.getIsActive());

        userRepo.save(existing);
    }


}
