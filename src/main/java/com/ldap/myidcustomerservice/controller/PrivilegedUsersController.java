package com.ldap.myidcustomerservice.controller;

import com.ldap.myidcustomerservice.service.PrivilegedUsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
//@RequestMapping("/api/users")
public class PrivilegedUsersController {

    @Autowired
    private PrivilegedUsersService userService;

//    @PostMapping("/register")
//    public ResponseEntity<User> registerUser(@RequestBody User user) {
//        User savedUser = userService.createUser(user);
//        return ResponseEntity.ok(savedUser);
//    }

    @PostMapping("/encode")
    public String encodePassword(@RequestParam String password) {
        return new BCryptPasswordEncoder().encode(password);
    }
}
