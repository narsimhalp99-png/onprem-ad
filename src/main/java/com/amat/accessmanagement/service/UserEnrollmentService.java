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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

        log.info("START createUser | employeeId={}", user.getEmployeeId());

        UserEntity saved = userRepo.save(user);

        log.info("END createUser | employeeId={}", user.getEmployeeId());

        return saved;
    }

    public Page<UserEntity> getUsers(Pageable pageable) {

        log.info(
                "START getUsers | page={} | size={}",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<UserEntity> page = userRepo.findAll(pageable);

        log.info(
                "END getUsers | totalElements={}",
                page.getTotalElements()
        );

        return page;
    }

    public UserEntity getUser(String employeeId, boolean additionalDetails) {

        log.info(
                "START getUser | employeeId={} | additionalDetails={}",
                employeeId,
                additionalDetails
        );

        UserEntity user = userRepo.findById(employeeId).orElse(null);

        if (user == null) {
            log.warn("User not found in DB | employeeId={}", employeeId);
            return null;
        }

        if (additionalDetails) {

            log.debug("Fetching additional LDAP details | employeeId={}", employeeId);

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

                List<Map<String, Object>> dataList =
                        (List<Map<String, Object>>) firstResp.get("data");

                String distinguishedName = (String) dataList.get(0).get("distinguishedName");

                if (!dataList.isEmpty() && distinguishedName != null) {
                    user.setRegularAccountDN(distinguishedName);
                    log.debug(
                            "Regular account DN resolved | employeeId={} | dn={}",
                            employeeId,
                            distinguishedName
                    );
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

                List<Map<String, Object>> newDataList =
                        (List<Map<String, Object>>) finalLdapResp.get("data");

                Map<String, Object> firstEntry = newDataList.get(0);

                String adminDN = (String) firstEntry.get("distinguishedName");

                if (!adminDN.isEmpty() && !adminDN.isBlank()) {
                    user.setAdminAccountDN(adminDN);
                    log.debug(
                            "Admin account DN resolved | employeeId={} | adminDn={}",
                            employeeId,
                            adminDN
                    );
                }

//                user.setLdapData(finalLdapResp);
            } catch (Exception e) {
                log.info("Exception while fetching LDAP details | employeeId={} | error={}", employeeId, e.getMessage(), e);
            }
        }

        log.info("END getUser | employeeId={}", employeeId);

        return user;
    }

    public UserEntity updateUser(String employeeId, UserEntity update) {

        log.info("START updateUser | employeeId={}", employeeId);

        UserEntity existing = getUser(employeeId, false);

        if (update.getDisplayName() != null)
            existing.setDisplayName(update.getDisplayName());

        existing.setFirstName(update.getFirstName());
        existing.setLastName(update.getLastName());
        existing.setOrganization(update.getOrganization());
        existing.setSubOrganization(update.getSubOrganization());
        existing.setTitle(update.getTitle());
        existing.setManagerEmpId(update.getManagerEmpId());
        existing.setIsActive(update.getIsActive());

        UserEntity saved = userRepo.save(existing);

        log.info("END updateUser | employeeId={}", employeeId);

        return saved;
    }

    public void deleteUser(String employeeId) {

        log.info("START deleteUser | employeeId={}", employeeId);

        UserEntity existing = getUser(employeeId, false);

        try {
            userRepo.delete(existing);
            userRepo.flush();
            log.info("User deleted successfully | employeeId={}", employeeId);
        } catch (Exception ex) {
            log.error(
                    "User deletion failed | employeeId={} | error={}",
                    employeeId,
                    ex.getMessage(),
                    ex
            );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "User deletion failed for employeeId: " + employeeId
            );
        }
    }

    public void updateUserDetails(String employeeId, UserEntity update) {

        log.info("START updateUserDetails | employeeId={}", employeeId);

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

        log.info("END updateUserDetails | employeeId={}", employeeId);
    }

}
