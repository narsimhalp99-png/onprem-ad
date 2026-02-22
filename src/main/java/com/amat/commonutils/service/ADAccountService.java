package com.amat.commonutils.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

@Service
@RequiredArgsConstructor
@Slf4j
public class ADAccountService {

    @Value("${spring.ldap.base:''}")
    String defaultBase;

    @Autowired
    LdapTemplate ldapTemplate;

    public DirContextOperations findUserBySamAccountName(String accountName) {

        String filter = "(sAMAccountName=" + accountName + ")";

        List<DirContextOperations> users = ldapTemplate.search(
                "",
                filter,
                SearchControls.SUBTREE_SCOPE,
                new ContextMapper<DirContextOperations>() {
                    @Override
                    public DirContextOperations mapFromContext(Object ctx) {
                        return (DirContextOperations) ctx;
                    }
                }
        );



        if (users.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        return users.get(0);
    }


    public Map<String, Object> checkAccountStatus(String accountName) {

        DirContextOperations user = findUserBySamAccountName(accountName);
        log.info("user::{}", user);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Object lockObj = user.getObjectAttribute("lockoutTime");
        Object uacObj = user.getObjectAttribute("userAccountControl");

        Long lockoutTime = (lockObj != null)
                ? Long.parseLong(lockObj.toString())
                : 0L;

        Integer uac = (uacObj != null)
                ? Integer.parseInt(uacObj.toString())
                : 0;

        boolean isLocked = lockoutTime > 0;
        boolean isDisabled = (uac & 2) != 0;

        return Map.of(
                "accountName", accountName,
                "locked", isLocked,
                "disabled", isDisabled,
                "enabled", !isDisabled
        );
    }

    @Transactional
    public void unlockAccount(String accountName) {

        DirContextOperations user = findUserBySamAccountName(accountName);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }


        Object uacObj = user.getObjectAttribute("userAccountControl");

        Integer uac = (uacObj != null)
                ? Integer.parseInt(uacObj.toString())
                : 0;
        boolean isDisabled = (uac & 2) != 0;


        if (isDisabled) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot unlock disabled account"
            );
        }

        user.setAttributeValue("lockoutTime", "0");
        ldapTemplate.modifyAttributes(user);

        log.info("Account unlocked | accountName={}", accountName);
    }

    @Transactional
    public void resetPassword(String accountName, String newPassword) throws Exception {

        DirContextOperations user = findUserBySamAccountName(accountName);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        Object uacObj = user.getObjectAttribute("userAccountControl");

        Integer uac = (uacObj != null)
                ? Integer.parseInt(uacObj.toString())
                : 0;
        boolean isDisabled = (uac & 2) != 0;


        if (isDisabled) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot reset password for disabled account"
            );
        }

        String quotedPassword = "\"" + newPassword + "\"";
        byte[] passwordBytes = quotedPassword.getBytes(StandardCharsets.UTF_16LE);

        ModificationItem[] mods = new ModificationItem[]{
                new ModificationItem(
                        DirContext.REPLACE_ATTRIBUTE,
                        new BasicAttribute("unicodePwd", passwordBytes)
                )
        };

        ldapTemplate.modifyAttributes(user.getDn(), mods);

        log.info("Password reset successfully | accountName={}", accountName);
    }


}