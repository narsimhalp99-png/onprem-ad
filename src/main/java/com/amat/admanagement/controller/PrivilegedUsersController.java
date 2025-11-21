package com.amat.admanagement.controller;

import com.amat.admanagement.service.PrivilegedUsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ad-management")
public class PrivilegedUsersController {

    @Autowired
    private PrivilegedUsersService userService;


    @PostMapping("/encode")
    public String encodePassword(@RequestParam String password) {
        return new BCryptPasswordEncoder().encode(password);
    }
}
