package com.amat.admanagement.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ad-management")
public class PrivilegedUsersController {


    @PostMapping("/encode")
    public String encodePassword(@RequestParam String password) {

        log.info("API HIT: encodePassword");

        log.debug(
                "Encoding password | length={}",
                password != null ? password.length() : 0
        );

        String encodedPassword = new BCryptPasswordEncoder().encode(password);

        log.info("Password encoded successfully");

        return encodedPassword;
    }
}
